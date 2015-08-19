(ns plastic.worker.editor.layout
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [react! dispatch main-dispatch dispatch-args]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [clojure.zip :as z]
            [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.editor.model :as editor]
            [plastic.worker.editor.layout.builder :refer [build-layout]]
            [plastic.worker.editor.layout.selections :refer [build-selections-render-info]]
            [plastic.worker.editor.layout.structural :refer [build-structural-web]]
            [plastic.worker.editor.layout.spatial :refer [build-spatial-web]]
            [plastic.util.zip :as zip-utils]
            [plastic.worker.editor.layout.utils :as utils]
            [plastic.worker.paths :as paths]))

(defn update-form-layout [editor-id form-loc]
  {:pre [(zip/node form-loc)
         (= (zip/tag (zip/up form-loc)) :forms)             ; parent has to be :forms
         (= 1 (count (node/children (zip/node (zip/up form-loc)))))]} ; root-loc is the only child
  (let [form-id (zip-utils/loc-id form-loc)
        layout (build-layout form-loc)
        selectables (utils/extract-all-selectables layout)
        spatial-web (build-spatial-web form-loc selectables)
        structural-web (build-structural-web form-loc)]
    (dispatch-args 0 [:editor-run-analysis editor-id form-id])
    (main-dispatch :editor-commit-layout editor-id form-id layout selectables spatial-web structural-web)))

(defn update-forms-layout-if-needed [editor form-locs]
  (let [reducer (fn [editor form-loc]
                  (let [form-node (z/node form-loc)
                        previously-layouted-node (editor/get-previously-layouted-form-node editor (:id form-node))]
                    (if (= previously-layouted-node form-node)
                      editor
                      (do
                        (update-form-layout (:id editor) form-loc)
                        (-> editor
                          (editor/prune-cache-of-previously-layouted-forms (map zip-utils/loc-id form-locs))
                          (editor/remember-previously-layouted-form-node form-node))))))]
    (reduce reducer editor form-locs)))

(defn update-layout [editors [selector]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (if-not (editor/parsed? editor)
        editor
        (let [independent-top-level-locs (map zip/down (map zip-utils/independent-zipper (editor/get-top-level-locs editor)))
              old-render-stae (editor/get-render-state editor)
              new-render-state {:order (map #(zip-utils/loc-id %) independent-top-level-locs)}]
          (if (not= old-render-stae new-render-state)
            (main-dispatch :editor-update-render-state (:id editor) new-render-state))
          (-> editor
            (editor/set-render-state new-render-state)
            (update-forms-layout-if-needed independent-top-level-locs)))))))

; ----------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-update-layout paths/editors-path update-layout)