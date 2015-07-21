(ns plastic.cogs.editor.render.code
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.render :refer [log-render]])
  (:require [plastic.cogs.editor.render.utils :refer [wrap-specials classv apply-shadowing-subscripts]]
            [plastic.cogs.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.cogs.editor.render.reusables :refer [raw-html-component]]
            [plastic.frame.core :refer [subscribe]]
            [plastic.util.helpers :as helpers]))

(declare code-block-component)

(defn code-token-component [editor-id form-id node-id]
  (let [selected? (subscribe [:editor-selection-node editor-id node-id])
        edited? (subscribe [:editor-editing-node editor-id node-id])
        cursor? (subscribe [:editor-cursor-node editor-id node-id])
        analysis (subscribe [:editor-form-node-analysis editor-id form-id node-id])
        layout (subscribe [:editor-form-node-layout editor-id form-id node-id])]
    (fn [_editor-id _form-id node-id]
      (log-render "code-token" [node-id (subs (:text @layout) 0 10)]
        (let [{:keys [selectable? type text id]} @layout
              {:keys [decl-scope call? def-name?]} @analysis
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
            (emit-token (-> text (apply-shadowing-subscripts (:shadows decl-scope))))))))))

(defn emit-code-block [editor-id form-id node-id]
  ^{:key node-id} [code-block-component editor-id form-id node-id])

(defn code-elements-row [emit hints items]
  (let [row (map emit items)]
    (if (:indent? hints)
      (cons [:div.indent] row)
      row)))

(defn code-elements-layout [emit [desc & lines]]
  (if (:oneliner? desc)
    [:div.elements
     (let [[hints & line-items] (first lines)]
       (code-elements-row emit hints line-items))]
    [:table.elements
     [:tbody
      (for [[index [hints & line-items]] (helpers/indexed-iteration lines)]
        (if (:double-column? hints)
          ^{:key index} [:tr
                         [:td (code-elements-row emit hints [(first line-items)])]
                         [:td (code-elements-row emit {} (rest line-items))]]
          ^{:key index} [:tr
                         [:td {:col-span 2} (code-elements-row emit hints line-items)]]))]]))

(defn code-element-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-form-node-layout editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (let [{:keys [tag children]} @layout]
        (condp = tag
          :newline [:span.newline "↵"]
          :token [code-token-component editor-id form-id node-id]
          (if children
            (code-elements-layout (partial emit-code-block editor-id form-id) children)
            [:span]))))))

(defn wrapped-code-element-component [editor-id form-id node-id _opener _closer]
  (let [selected? (subscribe [:editor-selection-node editor-id node-id])
        cursor? (subscribe [:editor-cursor-node editor-id node-id])
        layout (subscribe [:editor-form-node-layout editor-id form-id node-id])
        analysis (subscribe [:editor-form-node-analysis editor-id form-id node-id])]
    (fn [editor-id form-id node-id opener closer]
      (log-render "wrapper-code-block" node-id
        (let [{:keys [id selectable? depth tag]} @layout
              {:keys [new-scope?]} @analysis
              tag-name (name tag)]
          [:div.block {:data-qnid id
                       :class     (classv
                                    tag-name
                                    (if selectable? "selectable")
                                    (if (and selectable? @selected?) "selected")
                                    (if @cursor? "cursor")
                                    (if depth (str "depth-" depth))
                                    (if new-scope?
                                      (let [scope (get @analysis :scope)]
                                        (str "scope scope-" (:id scope) " scope-depth-" (:depth scope)))))}
           [:div.punctuation.opener opener]
           [code-element-component editor-id form-id node-id]
           [:div.punctuation.closer closer]])))))

(defn code-block-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-form-node-layout editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (log-render "code-block" node-id
        (let [wrapped-code-element (fn [& params] (vec (concat [wrapped-code-element-component editor-id form-id node-id] params)))]
          (condp = (:tag @layout)
            :list (wrapped-code-element "(" ")")
            :vector (wrapped-code-element "[" "]")
            :set (wrapped-code-element "#{" "}")
            :map (wrapped-code-element "{" "}")
            :fn (wrapped-code-element "#(" ")")
            :meta (wrapped-code-element "^" "")
            [code-element-component editor-id form-id node-id]))))))

(defn code-box-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-form-node-layout editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (log-render "code-box" node-id
        (let [child-id (first (:children @layout))]
          [:div.code-box
           [code-block-component editor-id form-id child-id]])))))