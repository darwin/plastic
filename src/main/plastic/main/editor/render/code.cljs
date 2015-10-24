(ns plastic.main.editor.render.code
  (:require-macros [plastic.logging :refer [log info warn error group group-end log-render]])
  (:require [plastic.main.editor.render.utils :refer [wrap-specials classv apply-shadowing-subscripts]]
            [plastic.main.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.main.editor.render.reusables :refer [raw-html-component]]
            [plastic.frame :refer [subscribe]]
            [plastic.util.helpers :as helpers]
            [plastic.main.editor.toolkit.id :as id]
            [plastic.util.dom :as dom]))

; -------------------------------------------------------------------------------------------------------------------

(defn code-token-component
  "A reagent component responsible for rendering a singe code token.

The component subscribes to relevant data which affects its visual appearance.
The component can present inline-editor in its place if in editing mode.

A hint: set `plastic.env.log-rendering` to log render calls into devtools console."
  [context editor-id unit-id node-id]
  (let [selected? (subscribe context [:editor-selection-node editor-id node-id])
        editing? (subscribe context [:editor-editing-node editor-id node-id])
        cursor? (subscribe context [:editor-cursor-node editor-id node-id])
        highlight? (subscribe context [:editor-highlight-node editor-id node-id])
        layout (subscribe context [:editor-layout-unit-node editor-id unit-id node-id])
        analysis (subscribe context [:editor-analysis-unit-node editor-id unit-id node-id])]
    (fn [context _editor-id _unit-id _node-id]
      (let [layout @layout
            _ (assert layout (str "missing layout for node " node-id))
            analysis @analysis
            selected? @selected?
            editing? @editing?
            cursor? @cursor?
            highlight? @highlight?]
        (log-render "code-token" [node-id layout analysis]
          (let [{:keys [selectable? tag text id]} layout
                {:keys [decl-scope call? def-name?]} analysis
                decl-classes (str (if (:decl? decl-scope) "decl ") "decl-scope decl-scope-" (:id decl-scope))
                props {:data-pnid id
                       :class     (classv
                                    (name tag)
                                    (if (and selectable? (not editing?)) "selectable")
                                    (if (and selectable? (not editing?) selected?) "selected")
                                    (if editing? "editing")
                                    (if cursor? "cursor")
                                    (if highlight? "highlighted")
                                    (if call? "call")
                                    (if decl-scope decl-classes)
                                    (if def-name? "def-name")
                                    (if (= tag :symbol)
                                      (if (= text "&")
                                        "ampersand"
                                        (if (= "_" (first text)) "silenced"))))}
                gen-html #(case tag
                           :string (wrap-specials (dom/escape-html text))
                           :linebreak "⌞"
                           (apply-shadowing-subscripts (dom/escape-html text) (:shadows decl-scope)))]
            [:div.token props
             (if editing?
               [inline-editor-component context id]
               [raw-html-component context (gen-html)])]))))))

(declare code-block-component)

(defn emit-code-block [context editor-id unit-id node-id]
  ^{:key node-id} [code-block-component context editor-id unit-id node-id])

(defn code-elements-row [emitter hints items]
  (let [row (map emitter items)]
    (if (:indent? hints)
      (cons [:div.indent] row)
      row)))

(defn split-line-items-into-two-columns [items]
  (let [linebreak-item (first items)                                                                                  ; double columns always have a linebreak as first id
        items-after-linebreak (rest items)
        spot-items (take-while id/spot? items-after-linebreak)
        items-after-spots (drop-while id/spot? items-after-linebreak)
        left-column-items (concat [linebreak-item] spot-items [(first items-after-spots)])
        right-column-items (rest items-after-spots)]
    [left-column-items right-column-items]))

(defn code-elements-layout [emitter [desc & lines]]
  (if (:oneliner? desc)
    [:div.elements
     (let [[hints & line-items] (first lines)]
       (code-elements-row emitter hints line-items))]
    [:table.elements
     [:tbody
      (for [[index [hints & line-items]] (helpers/indexed-iteration lines)]
        (if (:double-column? hints)
          (let [[left-items right-items] (split-line-items-into-two-columns line-items)]
            ^{:key index} [:tr
                           [:td (code-elements-row emitter hints left-items)]
                           [:td (code-elements-row emitter {} right-items)]])
          ^{:key index} [:tr
                         [:td {:col-span 2} (code-elements-row emitter hints line-items)]]))]]))

(defn code-element-component [_context _editor-id _unit-id _node-id _layout]
  (fn [context editor-id unit-id node-id layout]
    (let [{:keys [type children]} layout
          emitter (partial emit-code-block context editor-id unit-id)]
      (assert layout (str "missing layout for node " node-id))
      (case type
        :compound (code-elements-layout emitter children)
        [code-token-component context editor-id unit-id node-id]))))

(defn wrapped-code-element-component [context editor-id unit-id node-id _layout _opener _closer]
  (let [selected? (subscribe context [:editor-selection-node editor-id node-id])
        highlight-opener? (subscribe context [:editor-highlight-node editor-id (id/make node-id :opener)])
        highlight-closer? (subscribe context [:editor-highlight-node editor-id (id/make node-id :closer)])
        cursor? (subscribe context [:editor-cursor-node editor-id node-id])
        analysis (subscribe context [:editor-analysis-unit-node editor-id unit-id node-id])]
    (fn [context editor-id unit-id node-id layout opener closer]
      {:pre [(or opener closer)]}
      (let [analysis @analysis
            cursor? @cursor?
            highlight-opener? @highlight-opener?
            highlight-closer? @highlight-closer?]
        (log-render "wrapper-code-block" [node-id layout analysis]
          (let [{:keys [id selectable? depth tag after-nl]} layout
                {:keys [new-scope? scope]} analysis
                tag-name (name tag)
                scope-classes (str "scope scope-" (:id scope) " scope-depth-" (:depth scope))]
            [:div.block {:data-pnid id
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
             [code-element-component context editor-id unit-id node-id layout]
             (if closer
               [:div.punctuation.closer {:class (classv (if highlight-closer? "highlighted"))}
                closer])]))))))

(defn code-block-component [context editor-id unit-id node-id]
  (let [layout (subscribe context [:editor-layout-unit-node editor-id unit-id node-id])]
    (fn [context editor-id unit-id node-id]
      (let [layout @layout]
        (assert layout (str "missing layout for node " node-id))
        (log-render "code-block" [node-id layout]
          (let [{:keys [tag]} layout
                args [wrapped-code-element-component context editor-id unit-id node-id layout]
                wrapped-code-element (fn [& params] (vec (concat args params)))]
            (case tag
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
              [code-element-component context editor-id unit-id node-id layout])))))))

(defn code-box-component [context editor-id unit-id node-id]
  (let [layout (subscribe context [:editor-layout-unit-node editor-id unit-id node-id])
        code-visible (subscribe context [:settings :code-visible])]
    (fn [context editor-id unit-id node-id]
      (let [layout @layout
            code-visible @code-visible]
        (log-render "code-box" [node-id layout]
          (let [{:keys [children unit-kind]} layout]
            [:div.code-box {:class (classv (if unit-kind (str "unit-kind-" unit-kind)))}
             (if code-visible
               (let [child-id (first children)]
                 [code-block-component context editor-id unit-id child-id]))]))))))
