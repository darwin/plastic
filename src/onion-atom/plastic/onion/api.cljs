(ns plastic.onion.api
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:refer-clojure :exclude [atom]))

; TODO: nuke this namespace

; these apis will be provided by Atom during runtime

(defonce ^:dynamic *apis* nil)

(defonce ^:dynamic atom-api nil)
(defonce ^:dynamic File nil)
(defonce ^:dynamic Directory nil)
(defonce ^:dynamic TextEditor nil)
(defonce ^:dynamic $ nil)
(defonce ^:dynamic $$ nil)
(defonce ^:dynamic $$$ nil)
(defonce ^:dynamic info nil)

(defn register-apis! [apis]
  (set! *apis* apis)

  (set! atom-api (.-atom apis))
  (set! File (.-File apis))
  (set! Directory (.-Directory apis))
  (set! TextEditor (.-TextEditor apis))
  (set! $ (.-$ apis))
  (set! $$ (.-$$ apis))
  (set! $$$ (.-$$$ apis))
  (set! info (.-info apis)))