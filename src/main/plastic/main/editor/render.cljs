(ns plastic.main.editor.render
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [reagent.core :as reagent]
            [plastic.main.editor.render.editor :refer [editor-root-component]]))

; -------------------------------------------------------------------------------------------------------------------

(defn mount-editor [context element editor-id]
  (reagent/render [editor-root-component context editor-id] element))

(defn unmount-editor [context element]
  (reagent/unmount-component-at-node element))