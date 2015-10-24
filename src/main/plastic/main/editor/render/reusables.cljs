(ns plastic.main.editor.render.reusables
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.editor.render.utils :refer [dangerously-set-html]]))

; -------------------------------------------------------------------------------------------------------------------

(defn raw-html-component [_context html]
  [:div.raw (dangerously-set-html html)])