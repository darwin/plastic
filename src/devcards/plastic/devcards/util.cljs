(ns plastic.devcards.util
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [devcards.core :refer [card-base register-card] :refer-macros [reagent]]
            [cljs.pprint :refer [pprint]]
            [meld.parser :as parser]
            [meld.zip :as zip]
            [meld.support :refer [zipviz-component meldviz-component histogram-display histogram-component]]))

; -------------------------------------------------------------------------------------------------------------------

(defn markdown-source [source & [lang]]
  (str "```" (or lang "clojure") "\n" source "\n```"))

(defn pretty-print [v]
  (binding [*print-length* (* 16 1024)] (with-out-str (pprint v))))

; -------------------------------------------------------------------------------------------------------------------

(defn zip-card* [name doc loc-fn]
  (let [loc (loc-fn)]
    (card-base
      {:name          name
       :documentation doc
       :main-obj      (reagent zipviz-component)
       :initial-data  {:loc loc}
       :options       {:xwatch-atom true}})))

(defn meld-card* [name meld-fn]
  (let [meld (meld-fn)]
    (card-base
      {:name          name
       :documentation nil
       :main-obj      (reagent meldviz-component)
       :initial-data  {:meld meld}
       :options       {:xwatch-atom true}})))

(defn hist-card* [name meld-fn & [compounds?]]
  (let [meld (meld-fn)]
    (card-base
      {:name          name
       :documentation nil
       :main-obj      (reagent histogram-component)
       :initial-data  {:histogram (histogram-display meld 100 compounds?)}
       :options       {:xwatch-atom true}})))
