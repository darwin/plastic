(ns plastic.main.editor.render.code
  (:require-macros [plastic.logging :refer [log info warn error group group-end log-render]])
  (:require [plastic.main.editor.render.utils :refer [wrap-specials classv apply-shadowing-subscripts]]
            [plastic.main.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.main.editor.render.reusables :refer [raw-html-component]]
            [plastic.main.frame :refer [subscribe]]
            [plastic.util.helpers :as helpers]
            [plastic.main.editor.toolkit.id :as id]))

(declare code-block-component)

(defn code-token-component [editor-id form-id node-id]
  (let [selected? (subscribe [:editor-selection-node editor-id node-id])
        edited? (subscribe [:editor-editing-node editor-id node-id])
        cursor? (subscribe [:editor-cursor-node editor-id node-id])
        highlight? (subscribe [:editor-highlight-node editor-id node-id])
        analysis-subscription (subscribe [:editor-analysis-form-node editor-id form-id node-id])
        layout-subscription (subscribe [:editor-layout-form-node editor-id form-id node-id])]
    (fn [_editor-id _form-id node-id]
      (log-render "code-token" [node-id (subs (:text @layout-subscription) 0 10)]
        (let [{:keys [selectable? type text id]} @layout-subscription
              {:keys [decl-scope call? def-name?]} @analysis-subscription
              selected? @selected?
              edited? @edited?
              cursor? @cursor?
              highlight? @highlight?
              props (merge
                      {:data-qnid id
                       :class     (classv
                                    (if type (name type))
                                    (if (and selectable? (not edited?)) "selectable")
                                    (if (and selectable? (not edited?) selected?) "selected")
                                    (if cursor? "cursor")
                                    (if highlight? "highlighted")
                                    (if edited? "editing")
                                    (if call? "call")
                                    (if decl-scope
                                      (str (if (:decl? decl-scope) "decl ") "decl-scope decl-scope-" (:id decl-scope)))
                                    (if def-name? "def-name"))})
              emit-token (fn [html] [:div.token props
                                     (if edited?
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
          (let [[left-items right-items] (if (= (id/key-part (first line-items)) :spot)
                                           [[(first line-items) (second line-items)] (drop 2 line-items)]
                                           [[(first line-items)] (rest line-items)])]
            ^{:key index} [:tr
                           [:td (code-elements-row emit hints left-items)]
                           [:td (code-elements-row emit {} right-items)]])
          ^{:key index} [:tr
                         [:td {:col-span 2} (code-elements-row emit hints line-items)]]))]]))

(defn code-element-component [_editor-id _form-id _node-id _layout]
  (fn [editor-id form-id node-id layout]
    (let [{:keys [tag children]} layout]
      (condp = tag
        :newline [:span.newline "↵"]
        :token [code-token-component editor-id form-id node-id]
        (if children
          (code-elements-layout (partial emit-code-block editor-id form-id) children)
          [:span])))))

(defn wrapped-code-element-component [editor-id form-id node-id _layout _opener _closer]
  (let [selected? (subscribe [:editor-selection-node editor-id node-id])
        highlight-opener? (subscribe [:editor-highlight-node editor-id (id/make node-id :opener)])
        highlight-closer? (subscribe [:editor-highlight-node editor-id (id/make node-id :closer)])
        cursor? (subscribe [:editor-cursor-node editor-id node-id])
        analysis-subscription (subscribe [:editor-analysis-form-node editor-id form-id node-id])]
    (fn [editor-id form-id node-id layout opener closer]
      {:pre [(or opener closer)]}
      (log-render "wrapper-code-block" node-id
        (let [{:keys [id selectable? depth tag]} layout
              analysis @analysis-subscription
              {:keys [new-scope?]} analysis
              tag-name (name tag)
              cursor? @cursor?
              highlight-opener? @highlight-opener?
              highlight-closer? @highlight-closer?]
          [:div.block {:data-qnid id
                       :class     (classv
                                    tag-name
                                    (if selectable? "selectable")
                                    (if (and selectable? @selected?) "selected")
                                    (if cursor? "cursor")
                                    (if depth (str "depth-" depth))
                                    (if new-scope?
                                      (let [scope (get analysis :scope)]
                                        (str "scope scope-" (:id scope) " scope-depth-" (:depth scope)))))}
           (if opener
             [:div.punctuation.opener {:class (classv
                                                (if highlight-opener? "highlighted"))}
              opener])
           [code-element-component editor-id form-id node-id layout]
           (if closer
             [:div.punctuation.closer {:class (classv
                                                (if highlight-closer? "highlighted"))}
              closer])])))))

(defn code-block-component [editor-id form-id node-id]
  (let [layout-subscription (subscribe [:editor-layout-form-node editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (log-render "code-block" node-id
        (let [layout @layout-subscription
              wrapped-code-element (fn [& params] (vec (concat [wrapped-code-element-component editor-id form-id node-id layout] params)))]
          (condp = (:tag layout)
            :list (wrapped-code-element "(" ")")
            :vector (wrapped-code-element "[" "]")
            :set (wrapped-code-element "#{" "}")
            :map (wrapped-code-element "{" "}")
            :fn (wrapped-code-element "#(" ")")
            :meta (wrapped-code-element "^")
            :deref (wrapped-code-element "@")
            :quote (wrapped-code-element "'")
            :syntax-quote (wrapped-code-element "`")
            :unquote (wrapped-code-element "~")
            :unquote-splicing (wrapped-code-element "~@")
            [code-element-component editor-id form-id node-id layout]))))))

(defn code-box-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-layout-form-node editor-id form-id node-id])
        code-visible (subscribe [:settings :code-visible])]
    (fn [editor-id form-id node-id]
      (log-render "code-box" node-id
        [:div.code-box
         (if @code-visible
           (let [child-id (first (:children @layout))]
             [code-block-component editor-id form-id child-id]))]))))