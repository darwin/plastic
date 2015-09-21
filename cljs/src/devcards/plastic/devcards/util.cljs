(ns plastic.devcards.util
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [devcards.core :refer [defcard defcard* deftest reagent]]
                   [devcards.util.utils :refer [devcards-active?]])
  (:require [devcards.core :refer [card-base register-card]]
            [meld.parser :as parser]
            [meld.zip :as zip]
            [meld.support :refer [zipviz-component meldviz-component]]))

(defn def-zip-card [ns name source & [move]]
  (let [top-loc (zip/zip (parser/parse! source))
        loc (if move (move top-loc) top-loc)]
    (register-card {:path [ns name]
                    :func #(card-base
                            {:name          name
                             :documentation (str "```\n" source "\n```")
                             :main-obj      (reagent zipviz-component)
                             :initial-data  (atom {:loc loc})
                             :options       {}})})))

(defn def-meld-card [ns name source]
  (let [meld (parser/parse! source)]
    (register-card {:path [ns name]
                    :func #(card-base
                            {:name          name
                             :documentation nil
                             :main-obj      (reagent meldviz-component)
                             :initial-data  (atom {:meld meld})
                             :options       {}})})))


;`(devcards.core/defcard
;   ~name
;   (dc/reagent meld.support/meldviz-component)
;   {:meld ~meld}
;   {:padding false}))
