(ns plastic.env
  (:refer-clojure :exclude [or]))

; -------------------------------------------------------------------------------------------------------------------

(defmacro or [context & names]
  (let [resolved-names (map (fn [name] `(plastic.env.get ~context ~name)) names)]
    `(cljs.core/or ~@resolved-names)))