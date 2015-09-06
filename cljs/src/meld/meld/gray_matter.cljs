(ns meld.gray-matter
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [cljs.tools.reader.reader-types :as rt :refer [read-char unread get-line-number get-column-number]]
            [clojure.zip :as z]
            [meld.helpers :as helpers]
            [meld.comments :refer [stitch-aligned-comments]]
            [meld.zip :as zip]))

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
                        {:start  offset
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
  (let [* (fn [[current-offset table] next-gray-matter-offset]
            (helpers/skip-chunk reader (- next-gray-matter-offset current-offset))                                    ; advance reader to the next position of interest
            (let [gray-matter (read-gray-matter reader next-gray-matter-offset)
                  new-offset (or (:end (last gray-matter)) next-gray-matter-offset)
                  new-table (if-not (empty? gray-matter)
                              (assoc! table next-gray-matter-offset (stitch-aligned-comments gray-matter))            ; note comment stitching
                              table)]
              [new-offset new-table]))
        result (reduce * [0 (transient {})] offsets)]
    (assert (nil? (read-char reader)))                                                                                ; reader should be empty at this point
    (persistent! (second result))))                                                                                   ; return final table

(defn merge-gray-matter [root-loc table]
  (let [* (fn [loc [end-offset gray-matter]]
            (let [top-loc (zip/top loc)]
              (if (zero? end-offset)
                (zip/insert-childs top-loc gray-matter)                                                               ; special case of leading gray matter, see ***
                (let [all-locs (zip/take-all top-loc)
                      matching-locs (filter (partial zip/matching-end-loc? end-offset) all-locs)
                      best-loc (last matching-locs)]                                                                  ; we want to take outer-most sexpr closing at end-offset
                  (assert best-loc (str "no loc suitable for gray matter insertion at offset " end-offset))
                  (zip/insert-rights best-loc gray-matter)))))]
    (reduce * root-loc table)))

(defn process-gray-matter [node]
  (let [root-loc (zip/zipper node)
        all-locs (zip/take-all root-loc)
        all-ends (dedupe (sort (map zip/get-node-end all-locs)))                                                      ; nodes' ends are potential beginnings of gray matter sections
        all-ends-with-special (cons 0 all-ends)                                                                       ; a special case of leading gray matter, see ***
        reader (rt/indexing-push-back-reader (:source node))
        gray-matter-table (collect-gray-matter reader all-ends-with-special)                                          ; map node's end-offsets -> gray-matter sequences
        result-loc (merge-gray-matter root-loc gray-matter-table)]
    (z/root result-loc)))
