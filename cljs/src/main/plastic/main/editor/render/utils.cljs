(ns plastic.main.editor.render.utils
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [clojure.string :as string]
            [plastic.onion.api :refer [$]]))

(defonce ^:dynamic logger-indent 0)

(defn inc-indent []
  (set! logger-indent (inc logger-indent)))

(defn dec-indent []
  (set! logger-indent (dec logger-indent)))

(defn str-indent []
  (string/join (repeat logger-indent "  ")))

(defn wrap-specials [s]
  (-> s
    (string/replace #"\n" "<i>↵</i>\n")
    (string/replace #" " "<i>.</i>")
    (string/replace #"\t" "<i>»</i>")))

(defn dangerously-set-html [html]
  {:dangerouslySetInnerHTML {:__html html}})

(defn classv [& v]
  (string/join " " (filter (complement nil?) v)))

(defn wrap-in-span
  ([text class] (wrap-in-span true text class))
  ([pred text class] (if pred (str "<span class=\"" class "\">" text "</span>") text)))

(defn apply-shadowing-subscripts [text shadows]
  (if (>= shadows 2)
    (str text (wrap-in-span shadows "shadowed"))
    text))