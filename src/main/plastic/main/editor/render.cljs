(ns plastic.main.editor.render
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [reagent.core :as reagent]
            [plastic.main.editor.render.editor :refer [editor-root-component]]))

(defn mount-editor [element editor-id]
  (reagent/render [editor-root-component editor-id] element))

(defn unmount-editor [element]
  (reagent/unmount-component-at-node element))