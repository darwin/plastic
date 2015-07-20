(ns plastic.schema.frame
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end measure-time]]
                   [plastic.macros.glue :refer [react!]]
                   [reagent.ratom :refer [reaction]])
  (:require [plastic.frame.core :as frame]
            [clojure.string :as string]
            [cuerdas.core :as str]))

(defn simple-printer [acc val]
  (conj acc (cond
              (vector? val) "[…]"
              (map? val) "{…}"
              (set? val) "#{…}"
              (string? val) "\"…\""
              :else (pr-str val))))

(defn print-simple [vec]
  (str "[" (string/join " " (reduce simple-printer [] vec)) "]"))

(defn timing [handler]
  (fn timing-handler [db v]
    (measure-time (str "H! " (print-simple v))
      (handler db v))))

(defn register-handler
  ([id handler]
   (frame/register-handler id [timing frame/trim-v] handler))
  ([id middleware handler]
   (frame/register-handler id [timing frame/trim-v middleware] handler)))

(def subscribe frame/subscribe)