(ns meld.gray-matter
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [cljs.tools.reader.reader-types :as rt :refer [read-char unread get-line-number get-column-number]]
            [clojure.zip :as z]
            [meld.helpers :as helpers]
            [meld.comments :refer [stitch-aligned-comments]]
            [meld.zip :as zip]))

(defn read-gray-matter [reader start-offset]
  (let [tokens (transient [])]
    (loop [offset start-offset]
      (let [line (get-line-number reader)
            column (get-column-number reader)
            char (read-char reader)
            token (cond
                    (nil? char) nil
                    (helpers/whitespace? char) (helpers/slurp-whitespace reader char)
                    (helpers/newline? char) (helpers/slurp-newline reader char)
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

; gray matter is comments, linebreaks and whitespaces
(defn parse-gray-matter [text offset [line column]]
  (let [reader (rt/IndexingPushbackReader. (rt/string-push-back-reader text) line column (= column 1) nil 0 "")]
    (read-gray-matter reader offset)))

; -------------------------------------------------------------------------------------------------------------------

; we want to walk reader from left to right in one go for it to provide consistent line/column infos
(defn collect-gray-matter [reader offsets]
  (let [* (fn [[current-offset table] next-gray-matter-offset]
            (helpers/skip-chunk reader (- next-gray-matter-offset current-offset))                                    ; advance reader to position of interest
            (let [gray-matter (read-gray-matter reader next-gray-matter-offset)
                  new-offset (or (:end (last gray-matter)) next-gray-matter-offset)
                  new-table (if-not (empty? gray-matter)
                              (assoc table next-gray-matter-offset (stitch-aligned-comments gray-matter))
                              table)]
              [new-offset new-table]))
        result (reduce * [0 (sorted-map)] offsets)]
    (assert (nil? (read-char reader)))                                                                                ; reader should be empty at this point
    (second result)))                                                                                                 ; return final table

(defn merge-gray-matter-table [root-loc table]
  (let [* (fn [loc [end-offset gray-matter]]
            (let [top-loc (zip/top loc)]
              (if (zero? end-offset)
                (zip/insert-childs top-loc gray-matter)                                                               ; special case of leading gray matter, see ***
                (let [all-locs (take-while (complement z/end?) (iterate z/next top-loc))
                      matching-locs (filter (partial zip/matching-end-loc? end-offset) all-locs)
                      best-loc (last matching-locs)]
                  (assert best-loc)
                  (zip/insert-rights best-loc gray-matter)))))]
    (reduce * root-loc table)))

(defn process-gray-matter [node]
  (let [root-loc (zip/make-zipper node)
        all-locs (take-while (complement z/end?) (iterate z/next root-loc))
        all-ends (dedupe (sort (map #(:end (z/node %)) all-locs)))
        all-ends-with-beginning (cons 0 all-ends)                                                                     ; 0 for special case of leading gray matter, see ***
        reader (rt/indexing-push-back-reader (:source node))
        gray-matter-table (collect-gray-matter reader all-ends-with-beginning)
        new-loc (merge-gray-matter-table root-loc gray-matter-table)]
    (z/root new-loc)))

