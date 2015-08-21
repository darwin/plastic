(ns plastic.main.editor.model
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.helpers :as helpers]
            [reagent.ratom :refer [IDisposable dispose!]]
            [plastic.main.editor.toolkit.id :as id]))

(defprotocol IEditor)

(defrecord Editor [id def]
  IEditor)

(defn make [editor-id editor-def]
  (Editor. editor-id editor-def))

(defn valid-editor? [editor]
  (satisfies? IEditor editor))

(defn valid-editor-id? [editor-id]
  (pos? editor-id))

; -------------------------------------------------------------------------------------------------------------------

(defn get-id [editor]
  {:pre  [(valid-editor? editor)]
   :post [(valid-editor-id? %)]}
  (:id editor))

; -------------------------------------------------------------------------------------------------------------------

(defn register-reaction [editor reaction]
  {:pre [(valid-editor? editor)]}
  (update editor :reactions (fn [reactions] (conj (or reactions []) reaction))))

(defn dispose-reactions! [editor]
  {:pre [(valid-editor? editor)]}
  (doseq [reaction (:reactions editor)]
    (dispose! reaction))
  (dissoc editor :reactions))

; -------------------------------------------------------------------------------------------------------------------

(defn get-analysis-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:analysis form-id]))

(defn set-analysis-for-form [editor form-id new-analysis]
  {:pre [(valid-editor? editor)]}
  (let [old-analysis (get-analysis-for-form editor form-id)
        updated-analysis (helpers/overwrite-map old-analysis new-analysis)]
    (assoc-in editor [:analysis form-id] updated-analysis)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-layout-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:layout form-id]))

(defn set-layout-for-form [editor form-id new-layout]
  {:pre [(valid-editor? editor)]}
  (let [old-layout (get-layout-for-form editor form-id)
        updated-layout (helpers/overwrite-map old-layout new-layout)]
    (assoc-in editor [:layout form-id] updated-layout)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-selectables-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:selectables form-id]))

(defn set-selectables-for-form [editor form-id new-selectables]
  {:pre [(valid-editor? editor)]}
  (let [old-selectables (get-selectables-for-form editor form-id)
        updated-selectables (helpers/overwrite-map old-selectables new-selectables)]
    (assoc-in editor [:selectables form-id] updated-selectables)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-spatial-web-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:spatial-web form-id]))

(defn set-spatial-web-for-form [editor form-id new-spatial-web]
  {:pre [(valid-editor? editor)]}
  (let [old-spatial-web (get-spatial-web-for-form editor form-id)
        updated-spatial-web (helpers/overwrite-map old-spatial-web new-spatial-web)]
    (assoc-in editor [:spatial-web form-id] updated-spatial-web)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-structural-web-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:structural-web form-id]))

(defn set-structural-web-for-form [editor form-id new-structural-web]
  {:pre [(valid-editor? editor)]}
  (let [old-structural-web (get-structural-web-for-form editor form-id)
        updated-structural-web (helpers/overwrite-map old-structural-web new-structural-web)]
    (assoc-in editor [:structural-web form-id] updated-structural-web)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-render-state [editor]
  {:pre [(valid-editor? editor)]}
  (get editor :render-state))

(defn set-render-state [editor render-state]
  {:pre [(valid-editor? editor)]}
  (let [old-render-state (get-render-state editor)]
    (if (not= old-render-state render-state)
      (assoc-in editor [:render-state] render-state)
      editor)))

(defn get-render-order [editor]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:render-state :order]))

(defn set-render-order [editor order]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:render-state :order] order))

; -------------------------------------------------------------------------------------------------------------------

(defn get-selection [editor]
  {:pre  [(valid-editor? editor)]
   :post [(set? %)]}
  (or (get editor :selection) #{}))

(defn set-selection [editor selection]
  {:pre [(valid-editor? editor)
         (set? selection)]}
  (assoc editor :selection selection))

(defn toggle-selection [editor toggle-set]
  {:pre [(valid-editor? editor)
         (set? toggle-set)]}
  (let [toggler (fn [accum id]
                  (if (contains? accum id)
                    (disj accum id)
                    (conj accum id)))
        old-selection (get-selection editor)
        new-selection (reduce toggler old-selection toggle-set)]
    (set-selection editor new-selection)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-highlight [editor]
  {:pre  [(valid-editor? editor)]
   :post [(set? %)]}
  (or (get editor :highlight) #{}))

(defn set-highlight [editor highlight]
  {:pre [(valid-editor? editor)
         (set? highlight)]}
  (assoc editor :highlight highlight))

; -------------------------------------------------------------------------------------------------------------------

(defn selector-matches-editor? [editor-id selector]
  {:pre [(valid-editor-id? editor-id)]}
  (cond
    (vector? selector) (some #{editor-id} selector)
    (set? selector) (contains? selector editor-id)
    :default (= editor-id selector)))

(defn apply-to-editors [editors selector f]
  (let [reducer (fn [editors editor-id]
                  (or
                    (if (selector-matches-editor? editor-id selector)
                      (if-let [new-editor (f (get editors editor-id))]
                        (assoc editors editor-id new-editor)))
                    editors))]
    (reduce reducer editors (keys editors))))

; -------------------------------------------------------------------------------------------------------------------

(defn set-focused-form-id [editor form-id]
  {:pre [(valid-editor? editor)]}
  (assoc editor :focused-form-id (if form-id #{form-id} #{})))

(defn get-focused-form-id [editor]
  {:pre [(valid-editor? editor)
         (set? (or (get editor :focused-form-id) #{}))
         (<= (count (get editor :focused-form-id)) 1)]}
  (first (get editor :focused-form-id)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-editing [editor]
  {:pre [(valid-editor? editor)
         (set? (:editing editor))
         (<= (count (:editing editor)) 1)]}
  (first (:editing editor)))

(defn set-editing [editor node-id]
  {:pre [(valid-editor? editor)]}
  (if node-id
    (assoc editor :editing #{node-id})
    (assoc editor :editing #{})))

(defn editing? [editor]
  {:pre [(valid-editor? editor)]}
  (let [editing (:editing editor)]
    (and editing (not (empty? editing)))))

; -------------------------------------------------------------------------------------------------------------------

(defn get-top-level-form-ids [editor]
  {:pre [(valid-editor? editor)]}
  (get-render-order editor))

(defn get-first-form-id [editor]
  {:pre [(valid-editor? editor)]}
  (first (get-render-order editor)))

(defn get-last-form-id [editor]
  {:pre [(valid-editor? editor)]}
  (last (get-render-order editor)))

(defn get-first-selectable-token-id-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (let [spatial-web (get-spatial-web-for-form editor form-id)
        _ (assert spatial-web)
        first-line (second (first spatial-web))
        _ (assert (vector? first-line))]
    (:id (first first-line))))

(defn get-last-selectable-token-id-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (let [spatial-web (get-spatial-web-for-form editor form-id)
        _ (assert spatial-web)
        last-line (second (last spatial-web))
        _ (assert (vector? last-line))]
    (:id (peek last-line))))

(defn get-editor-in-db [db editor-id]
  {:post [(valid-editor? %)]}
  (get-in db [:editors editor-id]))

(defn update-in-db [db editor-id updater & args]
  {:pre [(valid-editor-id? editor-id)]}
  (let [old-editor (get-editor-in-db db editor-id)
        new-editor (apply updater old-editor args)]
    (assoc-in db [:editors editor-id] new-editor)))

(defn is-node-selectable? [editor form-id node-id]
  {:pre [(valid-editor? editor)]}
  (let [selectables (get-selectables-for-form editor form-id)]
    (not (nil? (get selectables node-id)))))

(defn is-node-present-in-form? [editor node-id form-id]
  {:pre [(valid-editor? editor)]}
  (let [layout (get-layout-for-form editor form-id)]
    (not (nil? (get layout node-id)))))

(defn get-form-id-for-node-id [editor node-id]
  {:pre [(valid-editor? editor)]}
  (let [form-ids (get-top-level-form-ids editor)]
    (some #(if (is-node-present-in-form? editor node-id %) %) form-ids)))

; -------------------------------------------------------------------------------------------------------------------
; cursor

(defn get-cursor [editor]
  {:pre [(valid-editor? editor)
         (set? (or (get editor :cursor) #{}))
         (<= (count (get editor :cursor)) 1)]}
  (first (get editor :cursor)))

(defn set-cursor [editor cursor]
  {:pre [(valid-editor? editor)]}
  (let [new-cursor (if cursor #{cursor} #{})
        new-cursor-form-id (if cursor (get-form-id-for-node-id editor cursor))]
    (-> editor
      (assoc :cursor new-cursor)
      (set-focused-form-id new-cursor-form-id))))

(defn replace-cursor-if-not-valid [editor new-cursor]
  {:pre [(valid-editor? editor)]}
  (let [cursor (get-cursor editor)
        focused-form-id (get-focused-form-id editor)]
    (if (is-node-selectable? editor focused-form-id cursor)
      editor
      (set-cursor editor new-cursor))))

; -------------------------------------------------------------------------------------------------------------------
; related rings

(defn find-related-ring [editor node-id]
  {:pre [(valid-editor? editor)]}
  ; TODO: in future searching for related ring should be across whole file, not just form-specific
  (let [form-id (get-form-id-for-node-id editor node-id)
        _ (assert form-id)
        analysis (get-analysis-for-form editor form-id)
        _ (assert analysis)
        info (get analysis node-id)]
    (or (:related info) #{})))

; -------------------------------------------------------------------------------------------------------------------
; inline-editor

(defn get-inline-editor [editor]
  {:pre [(valid-editor? editor)]}
  (get editor :inline-editor))

(defn set-inline-editor [editor new-state]
  {:pre [(valid-editor? editor)]}
  (assoc editor :inline-editor new-state))

(defn is-inline-editor-modified? [editor]
  {:pre [(valid-editor? editor)]}
  (:modified? (get-inline-editor editor)))

(defn is-inline-editor-empty? [editor]
  {:pre [(valid-editor? editor)]}
  (:empty? (get-inline-editor editor)))

(defn get-inline-editor-value [editor]
  {:pre [(valid-editor? editor)]}
  (:value (get-inline-editor editor)))

(defn get-inline-editor-mode [editor]
  {:pre [(valid-editor? editor)]}
  (:mode (get-inline-editor-value editor)))

(defn get-inline-editor-text [editor]
  {:pre [(valid-editor? editor)]}
  (:text (get-inline-editor-value editor)))