(ns plastic.main.editor.render.editor
  (:require [plastic.logging :refer-macros [log info warn error group group-end log-render]]
            [plastic.frame :refer [subscribe]:refer-macros [dispatch]]
            [plastic.main.editor.render.unit :refer [unit-component]]
            [plastic.main.editor.render.utils :refer [classv]]))

; -------------------------------------------------------------------------------------------------------------------

(defn handle-editor-click [context editor-id event]
  (.stopPropagation event)
  (dispatch context [:editor-clear-selection editor-id])
  (dispatch context [:editor-clear-cursor editor-id]))

(defn editor-root-component [context editor-id]
  (let [units (subscribe context [:editor-units editor-id])
        selections-debug-visible (subscribe context [:settings :selections-debug-visible])]
    (fn [context editor-id]
      (let [units @units
            selections-debug-visible @selections-debug-visible]
        (log-render "editor-root" editor-id
          [:div.plastic-editor                                                                                        ; .editor class is taken by Atom
           {:data-peid editor-id
            :class     (classv
                         (if selections-debug-visible "debug-selections"))
            :on-click  (partial handle-editor-click context editor-id)}
           [:div.units
            (for [unit-id units]
              ^{:key unit-id} [unit-component context editor-id unit-id])]])))))
