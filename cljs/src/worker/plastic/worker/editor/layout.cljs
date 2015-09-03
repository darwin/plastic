(ns plastic.worker.editor.layout
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [react! dispatch main-dispatch dispatch-args]]
                   [plastic.common :refer [process]])
  (:require [clojure.zip :as z]
            [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.editor.model :as editor]
            [plastic.worker.editor.layout.builder :refer [build-layout]]
            [plastic.worker.editor.layout.selections :refer [build-selections-render-info]]
            [plastic.worker.editor.layout.structural :refer [build-structural-web]]
            [plastic.worker.editor.layout.spatial :refer [build-spatial-web]]
            [plastic.util.zip :as zip-utils :refer [valid-loc?]]
            [plastic.worker.editor.layout.utils :as utils]
            [plastic.worker.paths :as paths]
            [plastic.util.helpers :refer [prepare-map-patch]]))

(defn update-form-layout [editor form-loc]
  {:pre [(valid-loc? form-loc)
         (zip-utils/form? form-loc)]}
  (let [editor-id (editor/get-id editor)
        form-id (zip-utils/loc-id form-loc)
        layout (build-layout form-loc)
        layout-patch (prepare-map-patch (editor/get-layout-for-form editor form-id) layout)
        selectables (utils/extract-all-selectables layout)
        selectables-patch (prepare-map-patch (editor/get-selectables-for-form editor form-id) selectables)
        spatial-web (build-spatial-web form-loc selectables)
        spatial-web-patch (prepare-map-patch (editor/get-spatial-web-for-form editor form-id) spatial-web)
        structural-web (build-structural-web form-loc layout)
        structural-web-patch (prepare-map-patch (editor/get-structural-web-for-form editor form-id) structural-web)]
    (dispatch-args 0 [:editor-run-analysis editor-id form-id])
    (main-dispatch :editor-commit-layout-patch editor-id form-id
      layout-patch selectables-patch spatial-web-patch structural-web-patch)
    (-> editor
      (editor/set-layout-for-form form-id layout)
      (editor/set-selectables-for-form form-id selectables)
      (editor/set-spatial-web-for-form form-id spatial-web)
      (editor/set-structural-web-for-form form-id structural-web))))

(defn update-forms-layout-if-needed [editor form-locs]
  (process form-locs editor
    (fn [editor form-loc]
      (let [form-node (z/node form-loc)
            previously-layouted-node (editor/get-previously-layouted-form-node editor (:id form-node))]
        (if (= previously-layouted-node form-node)
          editor
          (-> editor
            (update-form-layout form-loc)
            (editor/prune-cache-of-previously-layouted-forms (map zip-utils/loc-id form-locs))
            (editor/remember-previously-layouted-form-node form-node)))))))

(defn update-layout [editors [selector]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      {:pre [(editor/parsed? editor)]}
      (let [form-locs (editor/get-form-locs editor)
            independent-form-locs (map zip-utils/independent-zipper form-locs)
            old-render-state (editor/get-render-state editor)
            new-render-state {:order (map #(zip-utils/loc-id %) independent-form-locs)}]
        (if (not= old-render-state new-render-state)
          (main-dispatch :editor-update-render-state (:id editor) new-render-state))
        (-> editor
          (editor/set-render-state new-render-state)
          (update-forms-layout-if-needed independent-form-locs))))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-update-layout paths/editors-path update-layout)
