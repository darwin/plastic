(ns plastic.cogs.editor.render.utils
  (:require [clojure.string :as string])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

(defn wrap-specials [s]
  (-> s
    (string/replace #"\n" "<i>↵</i>\n")
    (string/replace #" " "<i>.</i>")
    (string/replace #"\t" "<i>»</i>")))

(defn dangerously-set-html [html]
  {:dangerouslySetInnerHTML {:__html html}})

(defn classv [& v]
  (string/join " " (filter (complement nil?) v)))