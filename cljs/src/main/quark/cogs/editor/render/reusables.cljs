(ns quark.cogs.editor.render.reusables
  (:require [quark.cogs.editor.render.utils :refer [raw-html]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn raw-html-component [html]
  [:div.raw (raw-html html)])