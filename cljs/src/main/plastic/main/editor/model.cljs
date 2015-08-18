(ns plastic.main.editor.model
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.helpers :as helpers]
            [reagent.ratom :refer [IDisposable dispose!]]
            [plastic.main.editor.toolkit.id :as id]))

(defn register-reaction [editor reaction]
  (update editor :reactions (fn [reactions] (conj (or reactions []) reaction))))

(defn dispose-reactions! [editor]
  (doseq [reaction (:reactions editor)]
    (dispose! reaction))
  (dissoc editor :reactions))

(defn get-analysis-for-form [editor form-id]
  (get-in editor [:analysis form-id]))

(defn set-analysis-for-form [editor form-id new-analysis]
  (let [old-analysis (get-analysis-for-form editor form-id)
        updated-analysis (helpers/overwrite-map old-analysis new-analysis)]
    (assoc-in editor [:analysis form-id] updated-analysis)))

(defn get-layout-for-form [editor form-id]
  (get-in editor [:layout form-id]))

(defn set-layout-for-form [editor form-id new-layout]
  (let [old-layout (get-layout-for-form editor form-id)
        updated-layout (helpers/overwrite-map old-layout new-layout)]
    (assoc-in editor [:layout form-id] updated-layout)))

(defn get-selectables-for-form [editor form-id]
  (get-in editor [:selectables form-id]))

(defn set-selectables-for-form [editor form-id new-selectables]
  (let [old-selectables (get-selectables-for-form editor form-id)
        updated-selectables (helpers/overwrite-map old-selectables new-selectables)]
    (assoc-in editor [:selectables form-id] updated-selectables)))

(defn get-spatial-web-for-form [editor form-id]
  (get-in editor [:spatial-web form-id]))

(defn set-spatial-web-for-form [editor form-id new-spatial-web]
  (let [old-spatial-web (get-spatial-web-for-form editor form-id)
        updated-spatial-web (helpers/overwrite-map old-spatial-web new-spatial-web)]
    (assoc-in editor [:spatial-web form-id] updated-spatial-web)))

(defn get-structural-web-for-form [editor form-id]
  (get-in editor [:structural-web form-id]))

(defn set-structural-web-for-form [editor form-id new-structural-web]
  (let [old-structural-web (get-structural-web-for-form editor form-id)
        updated-structural-web (helpers/overwrite-map old-structural-web new-structural-web)]
    (assoc-in editor [:structural-web form-id] updated-structural-web)))

(defn get-render-state [editor]
  (get editor :render-state))

(defn set-render-state [editor render-state]
  (let [old-render-state (get-render-state editor)]
    (if (not= old-render-state render-state)
      (assoc-in editor [:render-state] render-state)
      editor)))

(defn get-selection [editor]
  {:post [(set? %)]}
  (or (get editor :selection) #{}))

(defn get-cursor [editor]
  {:pre [(set? (or (get editor :cursor) #{}))
         (<= (count (get editor :cursor)) 1)]}
  (first (get editor :cursor)))

(defn cursor-and-selection-are-linked? [editor]
  (let [selection (get-selection editor)]
    (or (empty? selection) (and (= (count selection) 1) (= (first selection) (get-cursor editor))))))

(defn set-selection [editor selection]
  {:pre [(set? selection)]}
  (assoc editor :selection selection))

(defn toggle-selection [editor toggle-set]
  {:pre [(set? toggle-set)]}
  (let [toggler (fn [accum id]
                  (if (contains? accum id)
                    (disj accum id)
                    (conj accum id)))
        old-selection (get-selection editor)
        new-selection (reduce toggler old-selection toggle-set)]
    (set-selection editor new-selection)))

(defn selector-matches-editor? [editor-id selector]
  (cond
    (vector? selector) (some #{editor-id} selector)
    (set? selector) (contains? selector editor-id)
    :default (= editor-id selector)))

(defn apply-to-specified-editors [f editors id-or-ids]
  (apply array-map
    (flatten
      (for [[editor-id editor] editors]
        (if (selector-matches-editor? editor-id id-or-ids)
          [editor-id (f editor)]
          [editor-id editor])))))

(defn set-focused-form-id [editor form-id]
  (assoc editor :focused-form-id (if form-id #{form-id} #{})))

(defn get-focused-form-id [editor]
  {:pre [(set? (or (get editor :focused-form-id) #{}))
         (<= (count (get editor :focused-form-id)) 1)]}
  (first (get editor :focused-form-id)))

(defn get-id [editor]
  {:post [(pos? %)]}
  (:id editor))

(defn get-editing [editor]
  {:pre [(set? (:editing editor))
         (<= (count (:editing editor)) 1)]}
  (first (:editing editor)))

(defn set-editing [editor node-id]
  (if node-id
    (assoc editor :editing #{node-id})
    (assoc editor :editing #{})))

(defn editing? [editor]
  (let [editing (:editing editor)]
    (and editing (not (empty? editing)))))

(defn get-render-order [editor]
  (get-in editor [:render-state :order]))

(defn get-top-level-form-ids [editor]
  (get-render-order editor))

(defn get-first-form-id [editor]
  (first (get-render-order editor)))

(defn get-last-form-id [editor]
  (last (get-render-order editor)))

(defn get-first-selectable-token-id-for-form [editor form-id]
  (let [spatial-web (get-spatial-web-for-form editor form-id)
        _ (assert spatial-web)
        first-line (second (first spatial-web))
        _ (assert (vector? first-line))]
    (:id (first first-line))))

(defn get-last-selectable-token-id-for-form [editor form-id]
  (let [spatial-web (get-spatial-web-for-form editor form-id)
        _ (assert spatial-web)
        last-line (second (last spatial-web))
        _ (assert (vector? last-line))]
    (:id (peek last-line))))

(defn get-editor-in-db [db editor-id]
  (get-in db [:editors editor-id]))

(defn update-in-db [db editor-id updater & args]
  (let [old-editor (get-editor-in-db db editor-id)
        new-editor (apply updater old-editor args)]
    (assoc-in db [:editors editor-id] new-editor)))

(defn is-node-selectable? [editor form-id node-id]
  (let [selectables (get-selectables-for-form editor form-id)]
    (not (nil? (get selectables node-id)))))

(defn is-node-present-in-form? [editor node-id form-id]
  (let [layout (get-layout-for-form editor form-id)]
    (not (nil? (get layout node-id)))))

(defn get-form-id-for-node-id [editor node-id]
  (let [form-ids (get-top-level-form-ids editor)]
    (some #(if (is-node-present-in-form? editor node-id %) %) form-ids)))

(defn set-cursor [editor cursor & [link?]]
  (let [new-cursor (if cursor #{cursor} #{})
        new-cursor-form-id (if cursor (get-form-id-for-node-id editor cursor))]
    (cond-> editor
      (or link? (cursor-and-selection-are-linked? editor)) (assoc :selection new-cursor)
      true (assoc :cursor new-cursor)
      true (set-focused-form-id new-cursor-form-id))))

(defn replace-cursor-if-not-valid [editor new-cursor]
  (let [cursor (get-cursor editor)
        focused-form-id (get-focused-form-id editor)]
    (if (is-node-selectable? editor focused-form-id cursor)
      editor
      (set-cursor editor new-cursor))))