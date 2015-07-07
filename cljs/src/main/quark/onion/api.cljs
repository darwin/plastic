(ns quark.onion.api
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

; these apis will be provided by Atom during runtime, see :apis inface message

(defonce ^:dynamic *apis* nil)

(defonce ^:dynamic File nil)
(defonce ^:dynamic Directory nil)
(defonce ^:dynamic $ nil)
(defonce ^:dynamic $$ nil)
(defonce ^:dynamic $$$ nil)

(defn register-apis! [apis]
  (set! *apis* apis)

  (set! File (.-File apis))
  (set! Directory (.-Directory apis))
  (set! $ (.-$ apis))
  (set! $$ (.-$$ apis))
  (set! $$$ (.-$$$ apis)))