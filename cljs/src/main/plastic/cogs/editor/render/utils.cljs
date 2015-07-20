(ns plastic.cogs.editor.render.utils
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [clojure.string :as string]))

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