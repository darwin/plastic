(ns plastic.main.editor.model
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [dispatch worker-dispatch]]
                   [plastic.common :refer [process]])
  (:require [plastic.util.helpers :as helpers]
            [plastic.main.editor.toolkit.id :as id]
            [clojure.set :as set]))

(defprotocol IEditor)

(defrecord Editor [id]
  IEditor)

(extend-protocol IHash
  Editor
  (-hash [this] (goog/getUid this)))

(defn make [editor-id]
  (Editor. editor-id))

(defn valid-editor? [editor]
  (satisfies? IEditor editor))

(defn valid-editor-id? [editor-id]
  (pos? editor-id))

(declare update-highlight-and-puppets)
(declare get-form-id-for-node-id)

; -------------------------------------------------------------------------------------------------------------------

(defn get-id [editor]
  {:pre  [(valid-editor? editor)]
   :post [(valid-editor-id? %)]}
  (:id editor))

; -------------------------------------------------------------------------------------------------------------------

(defn get-uri [editor]
  {:pre  [(valid-editor? editor)]
   :post [(or (nil? %) (string? %))]}
  (get editor :uri))

(defn set-uri [editor uri]
  {:pre [(valid-editor? editor)
         (string? uri)]}
  (or
    (when (not= (get-uri editor) uri)
      (dispatch :editor-fetch-text (get-id editor))
      (assoc editor :uri uri))
    editor))

; -------------------------------------------------------------------------------------------------------------------

(defn get-analysis-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:analysis form-id]))

(defn set-analysis-for-form [editor form-id new-analysis]
  {:pre [(valid-editor? editor)]}
  (let [old-analysis (get-analysis-for-form editor form-id)
        updated-analysis (helpers/overwrite-map old-analysis new-analysis)]
    (-> editor
      (assoc-in [:analysis form-id] updated-analysis)
      (update-highlight-and-puppets))))

(defn set-analysis-patch-for-form [editor form-id patch]
  {:pre [(valid-editor? editor)]}
  (let [old-analysis (get-analysis-for-form editor form-id)
        patched-analysis (helpers/patch-map old-analysis patch)]
    (-> editor
      (assoc-in [:analysis form-id] patched-analysis)
      (update-highlight-and-puppets))))

; -------------------------------------------------------------------------------------------------------------------

(defn get-layout-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:layout form-id]))

(defn set-layout-for-form [editor form-id new-layout]
  {:pre [(valid-editor? editor)]}
  (let [old-layout (get-layout-for-form editor form-id)
        updated-layout (helpers/overwrite-map old-layout new-layout)]
    (assoc-in editor [:layout form-id] updated-layout)))

(defn set-layout-patch-for-form [editor form-id patch]
  {:pre [(valid-editor? editor)]}
  (let [old-layout (get-layout-for-form editor form-id)
        updated-layout (helpers/patch-map old-layout patch)]
    (assoc-in editor [:layout form-id] updated-layout)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-layout-for-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (let [form-id (get-form-id-for-node-id editor node-id)
        _ (assert form-id)
        form-layout (get-layout-for-form editor form-id)]
    (assert form-layout)
    (get form-layout node-id)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-selectables-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:selectables form-id]))

(defn set-selectables-for-form [editor form-id new-selectables]
  {:pre [(valid-editor? editor)]}
  (let [old-selectables (get-selectables-for-form editor form-id)
        updated-selectables (helpers/overwrite-map old-selectables new-selectables)]
    (assoc-in editor [:selectables form-id] updated-selectables)))

(defn set-selectables-patch-for-form [editor form-id patch]
  {:pre [(valid-editor? editor)]}
  (let [old-selectables (get-selectables-for-form editor form-id)
        updated-selectables (helpers/patch-map old-selectables patch)]
    (assoc-in editor [:selectables form-id] updated-selectables)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-spatial-web-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:spatial-web form-id]))

(defn set-spatial-web-for-form [editor form-id new-spatial-web]
  {:pre [(valid-editor? editor)]}
  (let [old-spatial-web (get-spatial-web-for-form editor form-id)
        updated-spatial-web (helpers/overwrite-map old-spatial-web new-spatial-web sorted-map)]
    (assoc-in editor [:spatial-web form-id] updated-spatial-web)))

(defn set-spatial-web-patch-for-form [editor form-id patch]
  {:pre [(valid-editor? editor)]}
  (let [old-spatial-web (get-spatial-web-for-form editor form-id)
        updated-spatial-web (helpers/patch-map old-spatial-web patch sorted-map)]
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

(defn set-structural-web-patch-for-form [editor form-id patch]
  {:pre [(valid-editor? editor)]}
  (let [old-structural-web (get-structural-web-for-form editor form-id)
        updated-structural-web (helpers/patch-map old-structural-web patch)]
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
  (let [old-selection (get-selection editor)
        new-selection (process toggle-set old-selection
                        (fn [accum id]
                          (if (contains? accum id)
                            (disj accum id)
                            (conj accum id))))]
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

(defn get-puppets [editor]
  {:pre  [(valid-editor? editor)]
   :post [(set? %)]}
  (or (get editor :puppets) #{}))

(defn set-puppets [editor puppets]
  {:pre [(valid-editor? editor)
         (set? puppets)]}
  (assoc editor :puppets puppets))

; -------------------------------------------------------------------------------------------------------------------

(defn selector-matches-editor? [editor-id selector]
  {:pre [(valid-editor-id? editor-id)]}
  (cond
    (vector? selector) (some #{editor-id} selector)
    (set? selector) (contains? selector editor-id)
    :default (= editor-id selector)))

(defn apply-to-editors [editors selector f & args]
  (process (keys editors) editors
    (fn [editors editor-id]
      (or
        (if (selector-matches-editor? editor-id selector)
          (if-let [new-editor (apply f (get editors editor-id) args)]
            (assoc editors editor-id new-editor)))
        editors))))

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
         (set? (or (:editing editor) #{}))
         (<= (count (:editing editor)) 1)]}
  (first (:editing editor)))

(defn set-editing [editor node-id]
  {:pre [(valid-editor? editor)]}
  (if node-id
    (if-not (= (get-editing editor) #{node-id})
      (assoc editor :editing #{node-id}))
    (if-not (= (get-editing editor) #{})
      (assoc editor :editing #{}))))

(defn editing? [editor]
  {:pre [(valid-editor? editor)]}
  (let [editing (:editing editor)]
    (and editing (not (empty? editing)))))

(defn layout-type->editor-mode [type]
  (condp = type
    :doc :string                                                                                                      ; doc nodes should be edited as strings for now
    :comment :string
    type))

(defn get-editing-setup [editor]
  {:pre [(valid-editor? editor)
         (editing? editor)]}
  (let [editing (get-editing editor)
        layout (get-layout-for-node editor editing)]
    {:text (:text layout)
     :type (:type layout)
     :mode (layout-type->editor-mode (:type layout))}))

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

(defn db-updater [editor-id f & args]
  (fn [db undo-summary]
    (update-in-db db editor-id
      (fn [editor]
        (let [report (some #(if (= (:editor-id %) editor-id) (:xform-report %)) undo-summary)]
          (assert report (str "unable to lookup report for editor #" editor-id ": " (pr-str undo-summary)))
          (apply f editor report args))))))

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
    (if (some #{node-id} form-ids)
      node-id
      (some #(if (is-node-present-in-form? editor node-id %) %) form-ids))))

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
      (set-focused-form-id new-cursor-form-id)
      (update-highlight-and-puppets))))

(defn find-valid-cursor-position [editor potential-editors]
  {:pre [(valid-editor? editor)]}
  (if-let [potential-editor (first potential-editors)]
    (when-let [potential-cursor (get-cursor potential-editor)]
      (if (is-node-selectable? editor (get-focused-form-id potential-editor) potential-cursor)
        (set-cursor editor potential-cursor)
        (recur editor (rest potential-editors))))))

(defn has-valid-cursor? [editor]
  (let [cursor (get-cursor editor)]
    (is-node-selectable? editor (get-focused-form-id editor) cursor)))

(defn move-cursor-to-valid-position-if-needed [editor potential-editors]
  (if (has-valid-cursor? editor)
    editor
    (if-let [editor-with-moved-cursor (find-valid-cursor-position editor potential-editors)]
      editor-with-moved-cursor
      (set-cursor editor nil))))

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

(defn get-inline-editor-initial-value [editor]
  {:pre [(valid-editor? editor)]}
  (:initial-value (get-inline-editor editor)))

(defn get-inline-editor-mode [editor]
  {:pre [(valid-editor? editor)]}
  (:mode (get-inline-editor-value editor)))

(defn get-inline-editor-text [editor]
  {:pre [(valid-editor? editor)]}
  (:text (get-inline-editor-value editor)))

(defn get-inline-editor-puppets-active? [editor]
  {:pre [(valid-editor? editor)]}
  (not (:puppets-deactivated? (get-inline-editor editor))))

(defn set-inline-editor-puppets-state [editor active?]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:inline-editor :puppets-deactivated?] (not active?)))

(defn get-inline-editor-puppets-effective? [editor]
  {:pre [(valid-editor? editor)]}
  (let [active? (get-inline-editor-puppets-active? editor)
        mode (get-inline-editor-mode editor)]
    (and (= mode :symbol) active?)))

; -------------------------------------------------------------------------------------------------------------------
; last xform

(defn get-xform-report [editor]
  {:pre [(valid-editor? editor)]}
  (let [report (get editor :xform-report)]
    (assert report)
    report))

(defn set-xform-report [editor report]
  {:pre [(valid-editor? editor)]}
  (assoc editor :xform-report report))


; -------------------------------------------------------------------------------------------------------------------
; highlight & puppets update

(defn update-highlight-and-puppets [editor]
  (if-let [cursor-id (get-cursor editor)]
    (let [id (id/id-part cursor-id)
          related-nodes (find-related-ring editor id)
          puppets (set/difference related-nodes #{cursor-id})
          spot-closer-node (if (id/spot? cursor-id) #{(id/make id :closer)})]
      (-> editor
        (set-highlight (set/union related-nodes spot-closer-node))
        (set-puppets puppets)))
    editor))

; -------------------------------------------------------------------------------------------------------------------

(defn remove-form [editor id]
  {:pre [id]}
  (-> editor
    (update :layout dissoc id)
    (update :selectables dissoc id)
    (update :spatial-web dissoc id)
    (update :structural-web dissoc id)
    (update :analysis dissoc id)))

(defn remove-forms [editor ids]
  {:pre [(valid-editor? editor)]}
  (process ids editor
    (fn [editor id]
      (remove-form editor id))))
