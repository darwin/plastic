(ns quark.frame.utils
  (:require
    [clojure.set :refer [difference]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

;; -- Misc --------------------------------------------------------------------

(defn first-in-vector
  [v]
  (if (vector? v)
    (first v)
    (error "re-frame: expected a vector event, but got: " v)))
