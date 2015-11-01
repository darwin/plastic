(ns plastic.main.editor.model
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.common :refer [process]])
  (:require [plastic.editor.model :as shared]
            [plastic.util.helpers :as helpers :refer [select-values]]
            [plastic.main.editor.toolkit.id :as id]
            [clojure.set :as set]))

; -------------------------------------------------------------------------------------------------------------------

(declare update-highlight-and-puppets)
(declare get-unit-id-for-node-id)

; -------------------------------------------------------------------------------------------------------------------
; shared functionality

(def make shared/make)
(def valid-editor? shared/valid-editor?)
(def valid-editor-id? shared/valid-editor-id?)

(def get-id shared/get-id)
(def apply-to-editors shared/apply-to-editors)
(def get-context shared/get-context)
(def set-context shared/set-context)
(def strip-context shared/strip-context)

(def subscribe! shared/subscribe!)
(def unsubscribe! shared/unsubscribe!)
(def unsubscribe-all! shared/unsubscribe-all!)

; -------------------------------------------------------------------------------------------------------------------

(defn get-uri [editor]
  {:pre  [(valid-editor? editor)]
   :post [(or (nil? %) (string? %))]}
  (get editor :uri))

(defn set-uri [editor uri]
  {:pre [(valid-editor? editor)
         (string? uri)]}
  (if (not= (get-uri editor) uri)
    (assoc editor :uri uri)
    editor))

; -------------------------------------------------------------------------------------------------------------------

(defn get-analysis-for-unit [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:analysis unit-id]))

(defn set-analysis-for-unit [editor unit-id new-analysis]
  {:pre [(valid-editor? editor)]}
  (let [old-analysis (get-analysis-for-unit editor unit-id)
        updated-analysis (helpers/overwrite-map old-analysis new-analysis)]
    (-> editor
      (assoc-in [:analysis unit-id] updated-analysis)
      (update-highlight-and-puppets))))

(defn set-analysis-patch-for-unit [editor unit-id patch]
  {:pre [(valid-editor? editor)]}
  (let [old-analysis (get-analysis-for-unit editor unit-id)
        patched-analysis (helpers/patch-map old-analysis patch)]
    (-> editor
      (assoc-in [:analysis unit-id] patched-analysis)
      (update-highlight-and-puppets))))

; -------------------------------------------------------------------------------------------------------------------

(defn get-layout-for-unit [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:layout unit-id]))

(defn set-layout-for-unit [editor unit-id new-layout]
  {:pre [(valid-editor? editor)]}
  (let [old-layout (get-layout-for-unit editor unit-id)
        updated-layout (helpers/overwrite-map old-layout new-layout)]
    (assoc-in editor [:layout unit-id] updated-layout)))

(defn set-layout-patch-for-unit [editor unit-id patch]
  {:pre [(valid-editor? editor)]}
  (let [old-layout (get-layout-for-unit editor unit-id)
        updated-layout (helpers/patch-map old-layout patch)]
    (assoc-in editor [:layout unit-id] updated-layout)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-layout-for-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (let [unit-id (get-unit-id-for-node-id editor node-id)
        _ (assert unit-id)
        unit-layout (get-layout-for-unit editor unit-id)]
    (assert unit-layout)
    (get unit-layout node-id)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-selectables-for-unit [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (let [layout (get-layout-for-unit editor unit-id)]
    (assert layout)
    (select-values :selectable? layout)))

(defn get-editables-for-unit [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (let [layout (get-layout-for-unit editor unit-id)]
    (assert layout)
    (select-values :editable? layout)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-spatial-web-for-unit [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:spatial-web unit-id]))

(defn set-spatial-web-for-unit [editor unit-id new-spatial-web]
  {:pre [(valid-editor? editor)]}
  (let [old-spatial-web (get-spatial-web-for-unit editor unit-id)
        updated-spatial-web (helpers/overwrite-map old-spatial-web new-spatial-web sorted-map)]
    (assoc-in editor [:spatial-web unit-id] updated-spatial-web)))

(defn set-spatial-web-patch-for-unit [editor unit-id patch]
  {:pre [(valid-editor? editor)]}
  (let [old-spatial-web (get-spatial-web-for-unit editor unit-id)
        updated-spatial-web (helpers/patch-map old-spatial-web patch sorted-map)]
    (assoc-in editor [:spatial-web unit-id] updated-spatial-web)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-structural-web-for-unit [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:structural-web unit-id]))

(defn set-structural-web-for-unit [editor unit-id new-structural-web]
  {:pre [(valid-editor? editor)]}
  (let [old-structural-web (get-structural-web-for-unit editor unit-id)
        updated-structural-web (helpers/overwrite-map old-structural-web new-structural-web)]
    (assoc-in editor [:structural-web unit-id] updated-structural-web)))

(defn set-structural-web-patch-for-unit [editor unit-id patch]
  {:pre [(valid-editor? editor)]}
  (let [old-structural-web (get-structural-web-for-unit editor unit-id)
        updated-structural-web (helpers/patch-map old-structural-web patch)]
    (assoc-in editor [:structural-web unit-id] updated-structural-web)))

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

(defn set-focused-unit-id [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (assoc editor :focused-unit-id (if unit-id #{unit-id} #{})))

(defn get-focused-unit-id [editor]
  {:pre [(valid-editor? editor)
         (set? (or (get editor :focused-unit-id) #{}))
         (<= (count (get editor :focused-unit-id)) 1)]}
  (first (get editor :focused-unit-id)))

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

(defn layout-editor-mode [layout]
  (let [tag (:tag layout)]
    (case tag
      :symbol :symbol
      :string :string
      :regexp :regexp
      :keyword :keyword
      :doc :string                                                                                                    ; doc nodes should be edited as strings for now, markdown later
      :comment :string)))

(defn get-editing-setup [editor]
  {:pre [(valid-editor? editor)
         (editing? editor)]}
  (let [editing (get-editing editor)
        layout (get-layout-for-node editor editing)
        layout-type (:type layout)]
    {:text (:text layout)
     :type layout-type
     :mode (layout-editor-mode layout)}))

; -------------------------------------------------------------------------------------------------------------------

(defn get-units [editor]
  {:pre [(valid-editor? editor)]}
  (get editor :units))

(defn set-units [editor unit-ids]
  {:pre [(valid-editor? editor)]}
  (if-not (identical? (get-units editor) unit-ids)
    (assoc editor :units unit-ids)
    editor))

; -------------------------------------------------------------------------------------------------------------------

(defn get-first-unit-id [editor]
  {:pre [(valid-editor? editor)]}
  (first (get-units editor)))

(defn get-last-unit-id [editor]
  {:pre [(valid-editor? editor)]}
  (last (get-units editor)))

(defn get-first-selectable-token-id-for-unit [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (let [spatial-web (get-spatial-web-for-unit editor unit-id)
        _ (assert spatial-web)
        first-line (second (first spatial-web))
        _ (assert (vector? first-line))]
    (:id (first first-line))))

(defn get-last-selectable-token-id-for-unit [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (let [spatial-web (get-spatial-web-for-unit editor unit-id)
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

(defn ^boolean is-node-selectable? [editor unit-id node-id]
  {:pre [(valid-editor? editor)]}
  (let [layout (get-layout-for-unit editor unit-id)
        node-layout (get layout node-id)]
    (:selectable? node-layout)))

(defn ^boolean is-node-editable? [editor unit-id node-id]
  {:pre [(valid-editor? editor)]}
  (let [layout (get-layout-for-unit editor unit-id)
        node-layout (get layout node-id)]
    (:editable? node-layout)))

(defn ^boolean is-node-present-in-unit? [editor node-id unit-id]
  {:pre [(valid-editor? editor)]}
  (let [layout (get-layout-for-unit editor unit-id)]
    (not (nil? (get layout node-id)))))

(defn get-unit-id-for-node-id [editor node-id]
  {:pre [(valid-editor? editor)]}
  (let [unit-ids (get-units editor)]
    (if (some #{node-id} unit-ids)
      node-id
      (some #(if (is-node-present-in-unit? editor node-id %) %) unit-ids))))

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
        new-cursor-unit-id (if cursor (get-unit-id-for-node-id editor cursor))]
    (-> editor
      (assoc :cursor new-cursor)
      (set-focused-unit-id new-cursor-unit-id)
      (update-highlight-and-puppets))))

(defn find-valid-cursor-position [editor potential-editors]
  {:pre [(valid-editor? editor)]}
  (if-let [potential-editor (first potential-editors)]
    (when-let [potential-cursor (get-cursor potential-editor)]
      (if (is-node-selectable? editor (get-focused-unit-id potential-editor) potential-cursor)
        (set-cursor editor potential-cursor)
        (recur editor (rest potential-editors))))))

(defn ^boolean has-valid-cursor? [editor]
  (let [cursor (get-cursor editor)]
    (is-node-selectable? editor (get-focused-unit-id editor) cursor)))

(defn move-cursor-to-valid-position-if-needed [editor potential-editors]
  (if (has-valid-cursor? editor)
    editor
    (if-let [editor-with-moved-cursor (find-valid-cursor-position editor potential-editors)]
      editor-with-moved-cursor
      (set-cursor editor nil))))

(defn ^boolean is-cursor-editable? [editor]
  (let [cursor (get-cursor editor)]
    (is-node-editable? editor (get-focused-unit-id editor) cursor)))

; -------------------------------------------------------------------------------------------------------------------
; related rings

(defn find-related-ring [editor node-id]
  {:pre [(valid-editor? editor)]}
  ; TODO: in future searching for related ring should be across whole file, not just unit-specific
  (or
    (if-let [unit-id (get-unit-id-for-node-id editor node-id)]
      (let [analysis (get-analysis-for-unit editor unit-id)
            info (get analysis node-id)]
        (:related info))) #{}))

; -------------------------------------------------------------------------------------------------------------------
; inline-editor

(defn get-inline-editor [editor]
  {:pre [(valid-editor? editor)]}
  (get editor :inline-editor))

(defn set-inline-editor [editor new-state]
  {:pre [(valid-editor? editor)]}
  (assoc editor :inline-editor new-state))

(defn ^boolean is-inline-editor-modified? [editor]
  {:pre [(valid-editor? editor)]}
  (:modified? (get-inline-editor editor)))

(defn ^boolean is-inline-editor-empty? [editor]
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

(defn ^boolean get-inline-editor-puppets-active? [editor]
  {:pre [(valid-editor? editor)]}
  (not (:puppets-deactivated? (get-inline-editor editor))))

(defn set-inline-editor-puppets-state [editor active?]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:inline-editor :puppets-deactivated?] (not active?)))

(defn ^boolean get-inline-editor-puppets-effective? [editor]
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
          spot-closer-node (if (id/spot? cursor-id) #{(id/make id :closer)})
          highlights (set/union related-nodes spot-closer-node)]
      (-> editor
        (set-highlight highlights)
        (set-puppets puppets)))
    editor))

; -------------------------------------------------------------------------------------------------------------------

(defn remove-unit [editor unit-id]
  {:pre [unit-id]}
  (-> editor
    (update :layout dissoc unit-id)
    (update :selectables dissoc unit-id)
    (update :spatial-web dissoc unit-id)
    (update :structural-web dissoc unit-id)
    (update :analysis dissoc unit-id)))

(defn remove-units [editor unit-ids]
  {:pre [(valid-editor? editor)]}
  (process unit-ids editor
    (fn [editor id]
      (remove-unit editor id))))
