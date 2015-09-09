(ns plastic.devcards
  (:require [plastic.logging :refer [log info warn error group group-end fancy-log*]]
            [devcards.core :as dc]))

(defmacro defmeldcard [source]
  (let [doc (str "```\n" source "\n```")]
    `(let [meld# (meld.parser/parse! ~source "*output of defmeldcard*")]
       (devcards.core/defcard ~doc meld#))))

(defmacro defhistcard [source compounds?]
  `(let [meld# (meld.parser/parse! ~source)
         histogram# (meld.support/histogram-display meld# 130 ~compounds?)]
     (devcards.core/defcard
       (dc/reagent (partial meld.support/histogram-component histogram#))
       {}
       {:padding false})))
