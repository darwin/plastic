(ns plastic.devcards
  (:require [plastic.logging :refer [log info warn error group group-end fancy-log*]]
            [devcards.core :as dc]))

(defmacro defmeldcard [source]
  (let [doc (str "```\n" source "\n```")]
    `(binding [meld.ids/*last-node-id!* (volatile! 0)]                                                                ; to have stable node ids for devcard purposes
       (let [meld# (meld.parser/parse! ~source)]
         (devcards.core/defcard ~doc meld#)))))

(defmacro defhistcard [name source compounds?]
  `(let [meld# (meld.parser/parse! ~source)
         histogram# (meld.support/histogram-display meld# 100 ~compounds?)]
     (devcards.core/defcard
       ~name
       (dc/reagent meld.support/histogram-component)
       {:histogram histogram#}
       {:padding false})))

(defmacro defmeldvizcard [name meld]
  `(devcards.core/defcard
     ~name
     (dc/reagent meld.support/meld-viz-component)
     {:meld ~meld}
     {:padding false}))
