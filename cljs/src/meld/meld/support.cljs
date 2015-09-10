(ns meld.support
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [clojure.string :as string]
            [meld.zip :as zip]
            [meld.node :as node]
            [meld.util :refer [update! indexed-react-keys]]
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

; -------------------------------------------------------------------------------------------------------------------

(defn histogram-component [data-atom]
  (let [{:keys [histogram]} @data-atom]
    [:div.meld-support
     [:pre.histogram histogram]]))

; -------------------------------------------------------------------------------------------------------------------

(defn enter-node [meld node]
  (let [[size _] (meld/get-compound-metrics meld node)
        source (node/get-source node)]
    {:id   (node/get-id node)
     :kind :open
     :text (subs source 0 size)}))

(defn leave-node [meld node]
  (let [[_ size] (meld/get-compound-metrics meld node)
        source (node/get-source node)]
    {:id   (node/get-id node)
     :kind :close
     :text (subs source (- (count source) size))}))

(defn process-node [node]
  {:id   (node/get-id node)
   :kind (or (node/get-tag node) (node/get-type node))
   :text (node/get-source node)})

(defn extract-leadspace [node]
  (if-let [whitespace (node/get-leadspace node)]
    {:id   (node/get-id node)
     :kind :whitespace
     :text whitespace}))

(defn extract-trailspace [node]
  (if-let [whitespace (node/get-trailspace node)]
    {:id   (node/get-id node)
     :kind :whitespace
     :text whitespace}))

(defn collect-visible-tokens [meld]
  (let [start-loc (zip/zip meld)]
    (remove nil? (zip/walk start-loc []
                   (fn [accum node op]
                     (-> accum
                       (conj (case op
                               (:enter :token) (extract-leadspace node)
                               :leave (extract-trailspace node)))
                       (conj (case op
                               :enter (enter-node meld node)
                               :leave (leave-node meld node)
                               :token (process-node node)))))))))

(defn emit-token [token]
  (let [{:keys [id kind text]} token]
    (case kind
      :linebreak [[:div.token.linebreak "↓"] [:br]]
      (mapcat identity
        (interpose [[:div.token.linebreak.inner "↓"] [:br]]
          (let [lines (string/split text #"\n")]
            (for [line lines]
              [[:div.token {:class kind :data-id id} line]])))))))

(defn meld-viz-component [data-atom]
  (let [{:keys [meld]} @data-atom
        source (meld/get-source meld)
        source-lines (string/split source #"\n")
        tokens (collect-visible-tokens meld)]
    [:div.meld-support
     [:div.meld-viz
      [:div.raw-source
       (indexed-react-keys
         (interpose [:br] (for [line source-lines]
                            [:pre.raw-line line])))]
      [:div.tokens
       (indexed-react-keys
         (mapcat emit-token tokens))]]]))
