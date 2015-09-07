(ns meld.gray-matter
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [cljs.tools.reader.reader-types :as rt :refer [read-char unread get-line-number get-column-number]]
            [clojure.zip :as z]
            [meld.helpers :as helpers]
            [meld.comments :refer [stitch-aligned-comments]]
            [meld.zip :as zip]
            [meld.node :as node]
            [meld.ids :as ids]))

; gray matter is whitespaces, linebreaks and comments
; standard tools.reader does not record them in parsed output
; we have to another post processing step to extract this information and include it in our parse-tree

(defn read-gray-matter [reader start-offset]
  (let [tokens (transient [])]
    (loop [offset start-offset]
      (let [line (get-line-number reader)
            column (get-column-number reader)
            char (read-char reader)
            token (cond
                    (nil? char) nil                                                                                   ; bail out early because newline? test would match it too
                    (helpers/whitespace? char) (helpers/slurp-whitespace reader char)
                    (helpers/linebreak? char) (helpers/slurp-linebreak reader char)
                    (= char ";") (helpers/slurp-comment reader char)
                    :else (do (unread reader char) nil))]
        (if token
          (let [end (+ offset (count (:source token)))
                token (merge token
                        {:id     (ids/next-node-id!)
                         :start  offset
                         :end    end
                         :line   line
                         :column column})]
            (conj! tokens token)
            (recur end))
          (persistent! tokens))))))

(defn measure-gray-matter [text]
  (let [len (alength text)
        skip-to-linebreak-or-end (fn [offset]
                                   (if (and (< offset len) (not (helpers/linebreak? (aget text offset))))             ; run to next linebreak (or end)
                                     (recur (inc offset))
                                     offset))]
    (loop [offset 0]
      (if (< offset len)
        (let [char (aget text offset)]
          (cond
            (helpers/whitespace? char) (recur (inc offset))
            (helpers/linebreak? char) (recur (inc offset))
            (= char ";") (recur (skip-to-linebreak-or-end (inc offset)))
            :else offset))))))

; -------------------------------------------------------------------------------------------------------------------

; we want to walk reader from left to right in one go for it to provide consistent line/column infos
(defn collect-gray-matter [reader offsets]
  (let [* (fn [[current-offset table*] next-gray-matter-offset]
            (helpers/skip-chunk reader (- next-gray-matter-offset current-offset))                                    ; advance reader to the next position of interest
            (let [gray-matter (read-gray-matter reader next-gray-matter-offset)
                  new-offset (or (:end (last gray-matter)) next-gray-matter-offset)
                  new-table (if-not (empty? gray-matter)
                              (assoc! table* next-gray-matter-offset (stitch-aligned-comments gray-matter))           ; note comment stitching
                              table*)]
              [new-offset new-table]))
        result-table* (second (reduce * [0 (transient {})] offsets))]
    (assert (nil? (read-char reader)))                                                                                ; reader should be empty at this point
    (persistent! result-table*)))                                                                                     ; return final table

(defn is-parent? [meld parent-id node-id]
  (loop [id node-id]
    (let [node (get meld id)
          parent (node/get-parent node)]
      (if-not parent
        false
        (if (= parent parent-id)
          true
          (recur parent))))))

(defn build-ends-lookup-table [meld]
  (let [table*! (volatile! (transient {}))]
    (doseq [[id node] meld]
      (if-let [end (:end node)]
        (let [prev-id (get @table*! end)]
          (if (or (nil? prev-id) (is-parent? meld id prev-id))
            (vswap! table*! assoc! end id)))))
    (persistent! @table*!)))

(defn merge-gray-matter [meld table]
  (let [ends-table (build-ends-lookup-table meld)
        * (fn [loc [end-offset gray-matter]]
            (if (zero? end-offset)
              (zip/insert-childs (zip/top loc) gray-matter)                                                           ; special case of leading gray matter, see ***
              (let [best-id (ends-table end-offset)
                    best-loc (zip/find loc best-id)]                                                                  ; we want to take outer-most sexpr closing at end-offset
                (zip/insert-rights best-loc gray-matter))))]
    (zip/unzip (reduce * (zip/zip meld) table))))

(defn process-gray-matter [meld source]
  (let [all-ends (dedupe (sort (keep node/get-end (vals meld))))                                                      ; nodes' ends are potential beginnings of gray matter sections
        all-ends-with-special (cons 0 all-ends)                                                                       ; a special case of leading gray matter, see ***
        reader (rt/indexing-push-back-reader source)
        gray-matter-table (collect-gray-matter reader all-ends-with-special)]                                         ; map node's end-offsets -> gray-matter sequences
    (merge-gray-matter meld gray-matter-table)))

