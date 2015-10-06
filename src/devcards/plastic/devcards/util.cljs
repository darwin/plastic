(ns plastic.devcards.util
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [devcards.core :refer [card-base register-card] :refer-macros [reagent]]
            [cljs.pprint :refer [pprint]]
            [meld.parser :as parser]
            [meld.zip :as zip]
            [meld.support :refer [zipviz-component meldviz-component histogram-display histogram-component]]))

(defn markdown-source [source & [lang]]
  (str "```" (or lang "clojure") "\n" source "\n```"))

(defn pretty-print [v]
  (binding [*print-length* (* 16 1024)] (with-out-str (pprint v))))

(defn with-stable-meld-ids [f & args]
  (binding [meld.ids/*last-node-id!* (volatile! 0)]
    (apply f args)))

(defn parse-with-stable-meld-ids [source]
  (with-stable-meld-ids parser/parse! source))

; -------------------------------------------------------------------------------------------------------------------

(defn def-zip-card* [ns name doc loc]
  (register-card {:path [ns name]
                  :func (fn []
                          (log "called card-base" name (count (zip/take-all zip/next loc)))
                          (card-base
                            {:name          name
                             :documentation doc
                             :main-obj      (reagent zipviz-component)
                             :initial-data  {:loc loc}
                             :options       {:xwatch-atom true}}))}))

;(defn def-zip-card* [ns name doc loc]
;  (register-card {:path [ns name]
;                  :func #(card-base
;                          {:name          name
;                           :documentation doc
;                           :main-obj      (reagent zipviz-component)
;                           :initial-data  (atom {:loc loc})
;                           :options       {}})}))

(defn def-source-zip-card* [ns name source & [move]]
  (let [top-loc (zip/zip (parse-with-stable-meld-ids source))
        loc (if move (move top-loc) top-loc)]
    (def-zip-card* ns name (markdown-source source) loc)))

(defn def-meld-card* [ns name source]
  (let [meld (parse-with-stable-meld-ids source)]
    (register-card {:path [ns name]
                    :func #(card-base
                            {:name          name
                             :documentation nil
                             :main-obj      (reagent meldviz-component)
                             :initial-data  (atom {:meld meld})
                             :options       {}})})))


(defn def-hist-card* [ns name source & [compounds?]]
  (let [meld (parse-with-stable-meld-ids source)
        histogram (histogram-display meld 100 compounds?)]
    (register-card {:path [ns name]
                    :func #(card-base
                            {:name          name
                             :documentation nil
                             :main-obj      (reagent histogram-component)
                             :initial-data  (atom {:histogram histogram})
                             :options       {}})})))

(defn def-meld-data-card* [ns name source]
  (let [meld (parse-with-stable-meld-ids source)]
    (register-card {:path [ns name]
                    :func #(card-base
                            {:name          name
                             :documentation (markdown-source source)
                             :main-obj      (markdown-source (pretty-print meld))
                             :initial-data  nil
                             :options       {}})})))
