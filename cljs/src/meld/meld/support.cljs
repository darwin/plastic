(ns meld.support
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [clojure.string :as string]
            [meld.zip :as zip]
            [meld.node :as node]
            [meld.util :refer [update!]]
            [meld.meld :as meld]))

(defn histogram [meld & [include-compounds?]]
  (let [start-loc (zip/zip meld)
        top-node (meld/get-top-node meld)
        [start end] (node/get-range top-node)
        init (vec (concat (repeat start 0) (repeat (- end start) 1)))
        hist$! (volatile! (transient init))]
    (loop [loc start-loc]
      (if (zip/end? loc)
        (persistent! @hist$!)
        (let [node (zip/node loc)
              [ra rb] (node/get-range node)]
          (if (or include-compounds? (not (node/compound? node)))
            (doseq [i (range ra rb)]
              (vswap! hist$! update! i inc)))
          (recur (zip/next loc)))))))

(defn format-as-text-block [text size]
  (let [re (js/RegExp. (str ".{1," size "}") "g")                                                                     ; http://stackoverflow.com/a/6259543/84283
        linear-text (string/replace text #"\n" "␤")]
    (.match linear-text re)))

(defn histogram-view [text hist chunk-size]
  (let [text-lines (format-as-text-block text chunk-size)
        hist-lines (format-as-text-block hist chunk-size)]
    (string/join "\n" (interleave text-lines hist-lines))))

; -------------------------------------------------------------------------------------------------------------------

(defn debug-print [meld label & [include-compounds?]]
  (let [hist (histogram meld include-compounds?)]
    (log (str "histogram" label ":"))
    (log (histogram-view (meld/get-source meld) (apply str hist) 40))
    meld))

(defn histogram-display [meld chunk-size & [include-compounds?]]
  (let [hist (histogram meld include-compounds?)]
    (histogram-view (meld/get-source meld) (apply str hist) chunk-size)))

(defn histogram-component [data-atom]
  (let [data @data-atom]
    [:div.meld-support
     [:pre.histogram (:histogram data)]]))
