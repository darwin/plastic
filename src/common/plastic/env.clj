(ns plastic.env
  (:refer-clojure :exclude [or])
  (:require [plastic.logging :refer [log info warn error group group-end fancy-log-with-time]]))

; -------------------------------------------------------------------------------------------------------------------

(defmacro or [context & names]
  (let [resolved-names (map (fn [name] `(plastic.env.get ~context ~name)) names)]
    `(cljs.core/or ~@resolved-names)))