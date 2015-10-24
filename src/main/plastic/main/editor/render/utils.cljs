(ns plastic.main.editor.render.utils
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [clojure.string :as string]))

; -------------------------------------------------------------------------------------------------------------------

(defn dangerously-set-html [html]
  {:dangerouslySetInnerHTML {:__html html}})

(defn classv [& v]
  (string/join " " (remove nil? v)))

(defn wrap-in-span
  ([text class] (wrap-in-span true text class))
  ([pred text class] (if pred (str "<span class=\"" class "\">" text "</span>") text)))

(defn apply-shadowing-subscripts [text shadows]
  (if (>= shadows 2)
    (str text (wrap-in-span shadows "shadowed"))
    text))

(defn wrap-specials [s]
  (-> s
    (string/replace #"\n" "<i>↵</i>\n")
    (string/replace #" " "<i>.</i>")
    (string/replace #"\t" "<i>»</i>")))

(defn fix-pre [s]
  (if (= (last s) "\n")
    (str s "&nbsp;")
    s))

(defn sections-to-class-names [parts]
  (string/join " " (keep (fn [[k v]]
                           {:pre [(keyword? k)]}
                           (if v (str "has-" (name k)))) parts)))
