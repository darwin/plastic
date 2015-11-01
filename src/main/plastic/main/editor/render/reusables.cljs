(ns plastic.main.editor.render.reusables
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.main.editor.render.utils :refer [dangerously-set-html]]))

; -------------------------------------------------------------------------------------------------------------------

(defn raw-html-component [_context html]
  [:div.raw (dangerously-set-html html)])