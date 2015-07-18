(ns plastic.cogs.editor.render.code
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.render.utils :refer [wrap-specials classv]]
            [plastic.cogs.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.cogs.editor.render.reusables :refer [raw-html-component]]
            [plastic.frame.core :refer [subscribe]]
            [plastic.onion.api :refer [$]]
            [plastic.cogs.editor.layout.utils :as utils]))

(declare code-block-component)

(defn apply-shadowing-subscripts [text shadows]
  (if (>= shadows 2)
    (str text (utils/wrap-in-span shadows "shadowed"))
    text))

(defn code-token-component [editor-id form-id node-id]
  (let [selected? (subscribe [:editor-selection-node editor-id node-id])
        edited? (subscribe [:editor-editing-node editor-id node-id])
        cursor? (subscribe [:editor-cursor-node editor-id node-id])
        analysis (subscribe [:editor-form-node-analysis editor-id form-id node-id])
        layout (subscribe [:editor-form-node-layout editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (let [{:keys [selectable? type text id]} @layout
            {:keys [decl-scope call? def-name?]} @analysis
            _ (log "R! token" id (subs text 0 10) "cursor" @cursor? @analysis)
            props (merge
                    {:data-qnid id
                     :class     (classv
                                  (if type (name type))
                                  (if (and selectable? (not @edited?)) "selectable")
                                  (if (and selectable? (not @edited?) @selected?) "selected")
                                  (if @cursor? "cursor")
                                  (if @edited? "editing")
                                  (if call? "call")
                                  (if decl-scope
                                    (str (if (:decl? decl-scope) "decl ") "decl-scope decl-scope-" (:id decl-scope)))
                                  (if def-name? "def-name"))})
            emit-token (fn [html] [:div.token props
                                   (if @edited?
                                     [inline-editor-component id text (or type :symbol)]
                                     [raw-html-component html])])]
        (condp = type
          :string (emit-token (-> text (wrap-specials)))
          (emit-token (-> text (apply-shadowing-subscripts (:shadows decl-scope)))))))))

(defn emit-code-block [editor-id form-id node-id]
  ^{:key node-id} [code-block-component editor-id form-id node-id])

(defn elements-row [emit hints items]
  (let [row (map emit items)]
    (if (:indent? hints)
      (cons [:div.indent] row)
      row)))

(defn elements-table [emit [desc & lines]]
  (if (:oneliner? desc)
    [:div.elements
     (let [[hints & line-items] (first lines)]
       (elements-row emit hints line-items))]
    [:table.elements
     [:tbody
      (for [[index [hints & line-items]] (map-indexed (fn [i l] [i l]) lines)]
        (if (:double-column? hints)
          ^{:key index} [:tr
                         [:td (elements-row emit hints [(first line-items)])]
                         [:td (elements-row emit {} (rest line-items))]]
          ^{:key index} [:tr
                         [:td {:col-span 2} (elements-row emit hints line-items)]]))]]))

(defn code-element-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-form-node-layout editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (let [{:keys [tag children]} @layout]
        (condp = tag
          :newline [:span.newline "↵"]
          :token [code-token-component editor-id form-id node-id]
          (elements-table (partial emit-code-block editor-id form-id) children))))))

(defn wrapped-code-element-component [editor-id form-id node-id opener closer]
  (let [selected? (subscribe [:editor-selection-node editor-id node-id])
        cursor? (subscribe [:editor-cursor-node editor-id node-id])
        layout (subscribe [:editor-form-node-layout editor-id form-id node-id])
        analysis (subscribe [:editor-form-node-analysis editor-id form-id node-id])]
    (fn [editor-id form-id node-id opener closer]
      (let [{:keys [id selectable? depth tag]} @layout
            {:keys [new-scope?]} @analysis
            tag-name (name tag)]
        (log "R! wrapper-code-block" id @analysis)
        [:div.block {:data-qnid id
                     :class     (classv
                                  tag-name
                                  (if selectable? "selectable")
                                  (if (and selectable? @selected?) "selected")
                                  (if @cursor? "cursor")
                                  (if depth (str "depth-" depth))
                                  (if new-scope? (str "scope scope-" (get-in @analysis [:scope :id]) " scope-depth-" (get-in @analysis [:scope :depth]))))}
         [:div.punctuation.opener opener]
         [code-element-component editor-id form-id node-id]
         [:div.punctuation.closer closer]]))))

(defn code-block-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-form-node-layout editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (log "R! code-block" @layout)
      (let [wrapped-code-element (fn [& params] (vec (concat [wrapped-code-element-component editor-id form-id node-id] params)))]
        (condp = (:tag @layout)
          :list (wrapped-code-element "(" ")")
          :vector (wrapped-code-element "[" "]")
          :set (wrapped-code-element "#{" "}")
          :map (wrapped-code-element "{" "}")
          :fn (wrapped-code-element "#(" ")")
          :meta (wrapped-code-element "^" "")
          [code-element-component editor-id form-id node-id])))))

(defn code-box-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-form-node-layout editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (log "R! code-box" @layout)
      (let [child-id (first (:children @layout))]
        [:div.code-box
         [code-block-component editor-id form-id child-id]]))))