(ns plastic.main.editor.render.code
  (:require-macros [plastic.logging :refer [log info warn error group group-end log-render]])
  (:require [plastic.main.editor.render.utils :refer [wrap-specials classv apply-shadowing-subscripts]]
            [plastic.main.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.main.editor.render.reusables :refer [raw-html-component]]
            [plastic.main.frame :refer [subscribe]]
            [plastic.util.helpers :as helpers]
            [plastic.main.editor.toolkit.id :as id]))

(declare code-block-component)

(defn code-token-component
  "A reagent component responsible for rendering a singe code token.

The component subscribes to relevant data which affects its visual appearance.
The component can present inline-editor in its place if in editing mode.

A hint: set `plastic.env.log-rendering` to log render calls into devtools console."
  [editor-id form-id node-id]
  (let [selected? (subscribe [:editor-selection-node editor-id node-id])
        editing? (subscribe [:editor-editing-node editor-id node-id])
        cursor? (subscribe [:editor-cursor-node editor-id node-id])
        highlight? (subscribe [:editor-highlight-node editor-id node-id])
        layout (subscribe [:editor-layout-form-node editor-id form-id node-id])
        analysis (subscribe [:editor-analysis-form-node editor-id form-id node-id])]
    (fn [_editor-id _form-id node-id]
      (log-render "code-token" [node-id (subs (:text @layout) 0 10)]
        (let [{:keys [selectable? type text id]} @layout
              {:keys [decl-scope call? def-name?]} @analysis
              selected? @selected?
              editing? @editing?
              cursor? @cursor?
              highlight? @highlight?
              decl-classes (str (if (:decl? decl-scope) "decl ") "decl-scope decl-scope-" (:id decl-scope))
              props {:data-qnid id
                     :class     (classv
                                  (if type (name type))
                                  (if (and selectable? (not editing?)) "selectable")
                                  (if (and selectable? (not editing?) selected?) "selected")
                                  (if editing? "editing")
                                  (if cursor? "cursor")
                                  (if highlight? "highlighted")
                                  (if call? "call")
                                  (if decl-scope decl-classes)
                                  (if def-name? "def-name"))}
              gen-html #(if (= type :string)
                         (wrap-specials text)
                         (apply-shadowing-subscripts text (:shadows decl-scope)))]
          [:div.token props
           (if editing?
             [inline-editor-component id]
             [raw-html-component (gen-html)])])))))

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
        (let [{:keys [id selectable? depth tag after-nl]} layout
              analysis @analysis-subscription
              {:keys [new-scope? scope]} analysis
              tag-name (name tag)
              cursor? @cursor?
              highlight-opener? @highlight-opener?
              highlight-closer? @highlight-closer?
              scope-classes (str "scope scope-" (:id scope) " scope-depth-" (:depth scope))]
          [:div.block {:data-qnid id
                       :class     (classv
                                    tag-name
                                    (if selectable? "selectable")
                                    (if (and selectable? @selected?) "selected")
                                    (if cursor? "cursor")
                                    (if depth (str "depth-" depth))
                                    (if new-scope? scope-classes)
                                    (if after-nl "after-nl"))}
           (if opener
             [:div.punctuation.opener {:class (classv (if highlight-opener? "highlighted"))}
              opener])
           [code-element-component editor-id form-id node-id layout]
           (if closer
             [:div.punctuation.closer {:class (classv (if highlight-closer? "highlighted"))}
              closer])])))))

(defn code-block-component [editor-id form-id node-id]
  (let [layout-subscription (subscribe [:editor-layout-form-node editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (log-render "code-block" node-id
        (let [layout @layout-subscription
              args [wrapped-code-element-component editor-id form-id node-id layout]
              wrapped-code-element (fn [& params] (vec (concat args params)))]
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
      (let [{:keys [children form-kind]} @layout]
        (log-render "code-box" node-id
          [:div.code-box {:class (classv (if form-kind (str "form-kind-" form-kind)))}
           (if @code-visible
             (let [child-id (first children)]
               [code-block-component editor-id form-id child-id]))])))))
