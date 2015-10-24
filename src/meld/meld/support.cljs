(ns meld.support
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [clojure.string :as string]
            [cljs.pprint :refer [pprint]]
            [meld.zip :as zip]
            [meld.node :as node]
            [meld.util :refer [update! indexed-react-keys]]
            [meld.core :as meld]
            [reagent.core :as reagent]))

; -------------------------------------------------------------------------------------------------------------------

(defn pretty-print [v]
  (binding [*print-length* 512] (with-out-str (pprint v))))

(defn dom-node-from-react [react-component]
  (let [dom-node (.getDOMNode react-component)]                                                                       ; TODO: deprecated!
    (assert dom-node)
    dom-node))

; -------------------------------------------------------------------------------------------------------------------

(defn histogram [meld & [include-compounds?]]
  (let [start-loc (zip/zip meld)
        top-node (meld/get-root-node meld)
        [start end] (node/get-range top-node)
        init (vec (concat (repeat start 0) (repeat (- end start) 1)))
        hist$! (volatile! (transient init))]
    (loop [loc start-loc]
      (if (zip/end? loc)
        (persistent! @hist$!)
        (let [node (zip/get-node loc)
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
    {:id    (node/get-id node)
     :kind  :open
     :tag   (node/get-tag node)
     :title #(pretty-print node)
     :text  (subs source 0 size)}))

(defn leave-node [meld node]
  (let [[_ size] (meld/get-compound-metrics meld node)
        source (node/get-source node)]
    {:id    (node/get-id node)
     :kind  :close
     :tag   (node/get-tag node)
     :title #(pretty-print node)
     :text  (subs source (- (count source) size))}))

(defn process-node [node]
  {:id    (node/get-id node)
   :kind  (node/get-tag node)
   :title #(pretty-print node)
   :text  (node/get-source node)})

(defn extract-leadspace [node]
  (if-let [whitespace (node/get-leadspace node)]
    {:id    (node/get-id node)
     :kind  :whitespace
     :title #(pretty-print node)
     :text  whitespace}))

(defn extract-trailspace [node]
  (if-let [whitespace (node/get-trailspace node)]
    {:id    (node/get-id node)
     :kind  :whitespace
     :title #(pretty-print node)
     :text  whitespace}))

(defn collect-visible-tokens [meld]
  (let [start-loc (zip/zip meld)]
    (remove nil?
      (zip/walk start-loc []
        (fn [accum node op]
          (-> accum
            (conj (case op
                    (:enter :token) (extract-leadspace node)
                    :leave (extract-trailspace node)))
            (conj (case op
                    :enter (enter-node meld node)
                    :leave (leave-node meld node)
                    :token (process-node node)))))))))

(defn token-tooltip-markup [content]
  (str "<pre class='title'>" content "</pre>"))

(defn setup-token-tipsy [title react-component]
  (let [dom-node (dom-node-from-react react-component)
        tooltip-spec #js {:gravity "s"
                          :opacity 1
                          :html    true
                          :title   #(token-tooltip-markup (title))}]
    (.tipsy (js/$ dom-node) tooltip-spec)))

(defn token-tipsy-component [tag props title line]
  (reagent/create-class
    {:component-did-mount  (partial setup-token-tipsy title)
     :component-did-update (partial setup-token-tipsy title)
     :reagent-render       (fn [] [tag props line])}))

(defn emit-token [token]
  (let [{:keys [id kind text title]} token]
    (case kind
      :linebreak [[:div.token.linebreak "↓"] [:br]]
      (mapcat identity
        (interpose [[:div.token.linebreak.inner "↓"] [:br]]
          (let [lines (string/split text #"\n")]
            (for [line lines]
              [[token-tipsy-component
                :div.token
                {:data-id id
                 :class   kind}
                title
                line]])))))))

(defn meldviz-component [data-atom]
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

; -------------------------------------------------------------------------------------------------------------------

(defn get-graph-node-shape [node]
  (case (node/get-type node)
    :compound "circle"
    "rect"))

(defn get-graph-node-class [_node selected? disabled?]
  (let [class-names [(if selected? "selected")
                     (if disabled? "disabled")]]
    (->> class-names
      (remove nil?)
      (interpose " ")
      (apply str))))

(defn get-graph-node-info [node selected? disabled?]
  {:label (node/get-desc node)
   :shape (get-graph-node-shape node)
   :title (pretty-print node)
   :class (get-graph-node-class node selected? disabled?)})

(defn get-graph-edge-info [_node _child]
  {:lineInterpolate "basis"})

(defn populate-graph-nodes! [graph start-loc selected-id]
  (let [top-id (zip/get-top-id start-loc)
        is-disabled? (fn [loc] (not (some #{top-id} (zip/ancestors loc))))]
    (loop [loc start-loc]
      (if-not (zip/end? loc)
        (let [node (zip/get-node loc)
              node-id (node/get-id node)
              selected? (= node-id selected-id)
              disabled? (is-disabled? loc)
              graph-node-info (get-graph-node-info node selected? disabled?)]
          (.setNode graph (node/get-id node) (clj->js graph-node-info))
          (recur (zip/next loc)))))))

(defn populate-graph-edges! [graph start-loc]
  (loop [loc start-loc]
    (when-not (zip/end? loc)
      (if (zip/branch? loc)
        (let [node (zip/get-node loc)
              node-id (node/get-id node)
              children (node/get-children node)]
          (doseq [child-id children]
            (let [graph-edge-info (get-graph-edge-info node child-id)]
              (.setEdge graph node-id child-id (clj->js graph-edge-info))))))
      (recur (zip/next loc)))))

(defn build-dag [loc]
  (let [graphlib (.-graphlib js/dagreD3)
        graph-class (.-Graph graphlib)
        graph (graph-class.)
        root-loc (zip/root loc)]
    (.setGraph graph #js {})
    (populate-graph-nodes! graph root-loc (zip/get-id loc))
    (populate-graph-edges! graph root-loc)
    graph))

(defn center-graph [graph root zoom scale]
  (let [root-width (js/parseInt (.style root "width") 10)
        graph-width (* (.-width (.graph graph)) scale)
        graph-height (* (.-height (.graph graph)) scale)
        center-offset (/ (- root-width graph-width) 2)]
    (doto zoom
      (.translate #js [center-offset 0])
      (.scale scale)
      (.event root))
    (.attr root "height" graph-height)))

(defn transform-str [translate scale]
  (str "translate(" translate ") " "scale (" scale ")"))

(defn make-zoom [root canvas]
  (let [d3 (.-d3 js/window)
        zoom-behavior (.zoom (.-behavior d3))
        handler (fn []
                  (let [event (.-event d3)]
                    (.attr canvas "transform" (transform-str (.-translate event) (.-scale event)))))
        zoom (.on zoom-behavior "zoom" handler)]
    (.call root zoom)
    zoom))

(defn style-tooltip [graph-node]
  (str "<pre class='title'>" (.-title graph-node) "</pre>"))

(defn setup-tooltips [graph root]
  (let [$nodes (.selectAll root ".node")]
    (.each $nodes
      (fn [graph-node-id]
        (this-as this
          (let [tooltip-spec #js {:gravity "w"
                                  :opacity 1
                                  :title   #(style-tooltip (.node graph graph-node-id))
                                  :html    true}]
            (.tipsy (js/$ this) tooltip-spec)))))))

(defn render-dag [graph dom-element]
  (let [d3 (.-d3 js/window)
        root (.select d3 dom-element)
        canvas (.select root ".canvas")
        renderer-class (.-render js/dagreD3)
        renderer (renderer-class)
        zoom (make-zoom root canvas)]
    (renderer canvas graph)
    (center-graph graph root zoom 0.75)
    (setup-tooltips graph root)))

(defn render-loc-graph [loc react-component]
  (if loc
    (let [graph (build-dag loc)
          dom-node (dom-node-from-react react-component)]
      (render-dag graph dom-node))))

(defn loc-graph-component [loc]
  (reagent/create-class
    {:component-did-mount  (partial render-loc-graph loc)
     :component-did-update (partial render-loc-graph loc)
     :reagent-render       (fn []
                             [:svg.root
                              [:g.canvas]])}))

(defn zipviz-component [data-atom]
  (let [{:keys [loc]} @data-atom]
    [:div.meld-support
     [:div.zip-viz
      [loc-graph-component loc]]]))
