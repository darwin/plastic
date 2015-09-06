(ns meld.debug
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [clojure.zip :as z]
            [clojure.string :as string]
            [meld.zip :as zip]))

(defn node-range [node]
  [(- (:start node) (count (:whitespace node))) (:end node)])

(defn histogram [node & [include-compounds?]]
  (let [loc (zip/zipper node)
        all-locs (zip/take-all loc)
        h (transient (vec (repeat (:end node) 0)))]
    (doseq [loc all-locs]
      (let [node (z/node loc)
            [ra rb] (node-range node)]
        (if (or include-compounds? (not (#{:compound} (:type node))))
          (doseq [i (range ra rb)]
            (assoc! h i (inc (nth h i)))))))
    (persistent! h)))

(defn format-as-text-block [text size]
  (let [re (js/RegExp. (str ".{1," size "}") "g")                                                                     ; http://stackoverflow.com/a/6259543/84283
        linear-text (string/replace text #"\n" "↵")]
    (.match linear-text re)))

(defn histogram-display [text hist]
  (let [chunk-size 40
        text-lines (format-as-text-block text chunk-size)
        hist-lines (format-as-text-block hist chunk-size)]
    (string/join "\n" (interleave text-lines hist-lines))))

(defn debug-print [node label & [include-compounds?]]
  (let [hist (histogram node include-compounds?)]
    (log (str "histogram" label ":"))
    (log (histogram-display (:source node) (apply str hist)))
    node))
