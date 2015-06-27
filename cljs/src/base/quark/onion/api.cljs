(ns quark.onion.api
  (:require [quark.cogs.renderer.core :refer [mount-editor]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [dispatch react!]]))

; these apis will be provided by Atom during runtime, see :apis inface message

(defonce ^:dynamic *apis* nil)

(defonce ^:dynamic File nil)
(defonce ^:dynamic Directory nil)

(defn register-apis! [apis]
  (set! *apis* apis)
  (set! File (.-File apis))
  (set! Directory (.-Directory apis)))
