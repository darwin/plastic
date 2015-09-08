(ns plastic.devcards
  (:require [plastic.logging :refer [log info warn error group group-end fancy-log*]]))

(defmacro defmeldcard [source]
  (let [doc (str "```\n" source "\n```")]
    `(let [meld# (meld.parser/parse! ~source "*output of defmeldcard*")]
       (devcards.core/defcard ~doc meld#))))
