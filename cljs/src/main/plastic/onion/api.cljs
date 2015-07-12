(ns plastic.onion.api
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:refer-clojure :exclude [atom]))

; these apis will be provided by Atom during runtime, see :apis inface message

(defonce ^:dynamic *apis* nil)

(defonce ^:dynamic atom nil)
(defonce ^:dynamic File nil)
(defonce ^:dynamic Directory nil)
(defonce ^:dynamic TextEditor nil)
(defonce ^:dynamic $ nil)
(defonce ^:dynamic $$ nil)
(defonce ^:dynamic $$$ nil)

(defn register-apis! [apis]
  (set! *apis* apis)

  (set! atom (.-atom apis))
  (set! File (.-File apis))
  (set! Directory (.-Directory apis))
  (set! TextEditor (.-TextEditor apis))
  (set! $ (.-$ apis))
  (set! $$ (.-$$ apis))
  (set! $$$ (.-$$$ apis)))