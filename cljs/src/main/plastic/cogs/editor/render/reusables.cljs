(ns plastic.cogs.editor.render.reusables
  (:require [plastic.cogs.editor.render.utils :refer [dangerously-set-html]])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

(defn raw-html-component [html]
  [:div.raw (dangerously-set-html html)])