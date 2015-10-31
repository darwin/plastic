(ns plastic.worker.editor.layout
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.frame :refer [dispatch main-dispatch]]
                   [plastic.common :refer [process]])
  (:require [plastic.worker.editor.model :as editor]
            [plastic.worker.editor.layout.builder :refer [build-layout]]
            [plastic.worker.editor.layout.selections :refer [build-selections-render-info]]
            [plastic.worker.editor.layout.structural :refer [build-structural-web]]
            [plastic.worker.editor.layout.spatial :refer [build-spatial-web]]
            [plastic.util.helpers :refer [prepare-map-patch select-values]]
            [clojure.set :as set]
            [meld.zip :as zip]
            [meld.node :as node]))

; -------------------------------------------------------------------------------------------------------------------

(defn update-unit-layout [editor unit-id]
  (let [editor-id (editor/get-id editor)
        unit-loc (zip/zip (editor/get-meld editor) unit-id)
        layout (build-layout unit-loc)
        layout-patch (prepare-map-patch (editor/get-layout-for-unit editor unit-id) layout)
        spatial-web (build-spatial-web unit-loc (select-values :selectable? layout))
        spatial-web-patch (prepare-map-patch (editor/get-spatial-web-for-unit editor unit-id) spatial-web)
        structural-web (build-structural-web unit-loc layout)
        structural-web-patch (prepare-map-patch (editor/get-structural-web-for-unit editor unit-id) structural-web)]
    (dispatch (editor/get-context editor) [:editor-run-analysis editor-id unit-id])                                   ; TODO: here should be async dispatch
    (main-dispatch (editor/get-context editor) [:editor-commit-layout-patch editor-id unit-id layout-patch
                                                spatial-web-patch structural-web-patch])
    (-> editor
      (editor/set-layout-for-unit unit-id layout)
      (editor/set-spatial-web-for-unit unit-id spatial-web)
      (editor/set-structural-web-for-unit unit-id structural-web))))

(defn update-forms-layout-if-needed [editor unit-ids]
  (let [editor (editor/prune-cache-of-previously-layouted-units editor unit-ids)]
    (process unit-ids editor
      (fn [editor unit-id]
        (let [old-node-revision (editor/get-previously-layouted-unit-revision editor unit-id)
              new-node (editor/get-node editor unit-id)
              new-node-revision (node/get-revision new-node)]
          (if (identical? old-node-revision new-node-revision)
            editor
            (-> editor
              (update-unit-layout unit-id)
              (editor/remember-previously-layouted-unit-revision unit-id new-node-revision))))))))

(defn update-layout [context db [editor-selector]]
  (editor/apply-to-editors context db editor-selector
    (fn [editor]
      {:pre [(editor/has-meld? editor)]}
      (let [editor-id (editor/get-id editor)
            new-units (editor/get-unit-ids editor)
            old-units (editor/get-units editor)
            removed-unit-ids (set/difference (set old-units) (set new-units))]
        (if-not (empty? removed-unit-ids)
          (main-dispatch context [:editor-remove-units editor-id removed-unit-ids]))
        (if (not= old-units new-units)
          (main-dispatch context [:editor-update-units editor-id new-units]))
        (-> editor
          (editor/remove-units removed-unit-ids)
          (editor/set-units new-units)
          (update-forms-layout-if-needed new-units))))))
