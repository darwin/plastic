(ns quark.cogs.editor.model
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn get-selections [editor]
  (or (get-in editor [:selections]) {}))

(defn set-selections [editor selections]
  (assoc-in editor [:selections] selections))

(defn get-focused-form-id [editor]
  (get-in editor [:selections :focused-form-id]))

(defn get-focused-selection [editor]
  (let [selections (get-selections editor)
        focused-form-id (get-focused-form-id editor)]
  (get selections focused-form-id)))

(defn set-render-state [editor render-state]
  (assoc-in editor [:render-state] render-state))

(defn get-id [editor]
  (:id editor))

(defn get-editing-set [editor]
  {:post [(set? %)]}
  (or (:editing editor) #{}))

(defn set-editing-set [editor editing-set]
  {:pre [(or (nil? editing-set) (set? editing-set))]}
  (assoc editor :editing (or editing-set #{})))

(defn editing? [editor]
  (let [editing (:editing editor)]
    (and editing (not (empty? editing)))))