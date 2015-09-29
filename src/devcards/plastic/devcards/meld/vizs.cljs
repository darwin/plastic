(ns plastic.devcards.meld.vizs
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.devcards.util :refer-macros [def-meld-card]]))

(def card-ns :meld.viz)

(def-meld-card card-ns "bad-case" " (0   )  ")

(def-meld-card card-ns "small-vec" "[0 1 2]")

(def-meld-card card-ns "simple-fn" "(ns n1.simplefn)

(defn fn1 [p1]
  (fn fn2 [p2]
    (use p1 p2)))")

(def-meld-card card-ns "some-doc-example" "(ns n1.doc)

; independent comment
; block spanning
; 3 lines


; -----------------------
; under separator

(defn log-ex
  \"Middleware which catches and prints any handler-generated exceptions to console.
  Handlers are called from within a core.async go-loop, and core.async produces
  a special kind of hell when in comes to stacktraces. By the time an exception
  has passed through a go-loop its stack is mangled beyond repair and you'll
  have no idea where the exception was thrown.
  So this middleware catches and prints to stacktrace before the core.async sausage
  machine has done its work.
  \"
  [handler]
  (fn log-ex-handler
    [db v] ; looooooooooooong comment sdaadjasidiasodjaosd saijdoiad joadj asdj iosjdas ijd iosajdoasjd asj
    (warn \"re-frame: use of \\\"log-ex\\\" is deprecated.\")

    (try
      ; inlined comment
      (handler db v)
      (catch :default e                                     ;; ooops, handler threw
        (do
          (.error js/console (.-stack e))  ; side-comment
          ; spanning multiple lines
          (throw e)))))) ; more

; comment after
; in block

; another comment

(def test \"some string\") ; test comment
")

(def-meld-card card-ns "real-code" "(ns plastic.main.editor.render.code
  (:require-macros [plastic.logging :refer [log info warn error group group-end log-render]])
  (:require [plastic.main.editor.render.utils :refer [wrap-specials classv apply-shadowing-subscripts]]
            [plastic.main.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.main.editor.render.reusables :refer [raw-html-component]]
            [plastic.main.frame :refer [subscribe]]
            [plastic.util.helpers :as helpers]
            [plastic.main.editor.toolkit.id :as id]
            [plastic.util.dom :as dom]))

(defn code-token-component
  \"A reagent component responsible for rendering a singe code token.

The component subscribes to relevant data which affects its visual appearance.
The component can present inline-editor in its place if in editing mode.

A hint: set `plastic.env.log-rendering` to log render calls into devtools console.\"
  [editor-id form-id node-id]
  (let [selected? (subscribe [:editor-selection-node editor-id node-id])
        editing? (subscribe [:editor-editing-node editor-id node-id])
        cursor? (subscribe [:editor-cursor-node editor-id node-id])
        highlight? (subscribe [:editor-highlight-node editor-id node-id])
        layout (subscribe [:editor-layout-form-node editor-id form-id node-id])
        analysis (subscribe [:editor-analysis-form-node editor-id form-id node-id])]
    (fn [_editor-id _form-id _node-id]
      (log-render \"code-token\" [@layout @analysis]
        (let [{:keys [selectable? type text id]} @layout
              {:keys [decl-scope call? def-name?]} @analysis
              selected? @selected?
              editing? @editing?
              cursor? @cursor?
              highlight? @highlight?
              decl-classes (if decl-scope
                             (str (if (:decl? decl-scope) \"decl \") \"decl-scope decl-scope-\" (:id decl-scope)))
              props {:data-pnid id
                     :class     (classv
                                  (name type)
                                  (if (and selectable? (not editing?)) \"selectable\")
                                  (if (and selectable? (not editing?) selected?) \"selected\")
                                  (if editing? \"editing\")
                                  (if cursor? \"cursor\")
                                  (if highlight? \"highlighted\")
                                  (if call? \"call\")
                                  (if decl-scope decl-classes)
                                  (if def-name? \"def-name\")
                                  (if (= type :symbol)
                                    (if (= text \"&\")
                                      \"ampersand\"
                                      (if (= \"_\" (first text)) \"silenced\"))))}
              gen-html #(condp = type
                         :string (wrap-specials (dom/escape-html text))
                         :linebreak \"⌞\"
                         (apply-shadowing-subscripts (dom/escape-html text) (:shadows decl-scope)))]
          [:div.token props
           (if editing?
             [inline-editor-component id]
             [raw-html-component (gen-html)])])))))

(declare code-block-component)

(defn emit-code-block [editor-id form-id node-id]
  ^{:key node-id} [code-block-component editor-id form-id node-id])

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

(defn code-element-component [_editor-id _form-id _node-id _layout]
  (fn [editor-id form-id node-id layout]
    (let [{:keys [tag children]} layout
          emitter (partial emit-code-block editor-id form-id)]
      (condp = tag
        :token [code-token-component editor-id form-id node-id]
        (if children
          (code-elements-layout emitter children)
          [:span])))))

(defn wrapped-code-element-component [editor-id form-id node-id _layout _opener _closer]
  (let [selected? (subscribe [:editor-selection-node editor-id node-id])
        highlight-opener? (subscribe [:editor-highlight-node editor-id (id/make node-id :opener)])
        highlight-closer? (subscribe [:editor-highlight-node editor-id (id/make node-id :closer)])
        cursor? (subscribe [:editor-cursor-node editor-id node-id])
        analysis-subscription (subscribe [:editor-analysis-form-node editor-id form-id node-id])]
    (fn [editor-id form-id node-id layout opener closer]
      {:pre [(or opener closer)]}
      (log-render \"wrapper-code-block\" node-id
        (let [{:keys [id selectable? depth tag after-nl]} layout
              analysis @analysis-subscription
              {:keys [new-scope? scope]} analysis
              tag-name (name tag)
              cursor? @cursor?
              highlight-opener? @highlight-opener?
              highlight-closer? @highlight-closer?
              scope-classes (str \"scope scope-\" (:id scope) \" scope-depth-\" (:depth scope))]
          [:div.block {:data-pnid id
                       :class     (classv
                                    tag-name
                                    (if selectable? \"selectable\")
                                    (if (and selectable? @selected?) \"selected\")
                                    (if cursor? \"cursor\")
                                    (if depth (str \"depth-\" depth))
                                    (if new-scope? scope-classes)
                                    (if after-nl \"after-nl\"))}
           (if opener
             [:div.punctuation.opener {:class (classv (if highlight-opener? \"highlighted\"))}
              opener])
           [code-element-component editor-id form-id node-id layout]
           (if closer
             [:div.punctuation.closer {:class (classv (if highlight-closer? \"highlighted\"))}
              closer])])))))

(defn code-block-component [editor-id form-id node-id]
  (let [layout-subscription (subscribe [:editor-layout-form-node editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (log-render \"code-block\" node-id
        (let [layout @layout-subscription
              args [wrapped-code-element-component editor-id form-id node-id layout]
              wrapped-code-element (fn [& params] (vec (concat args params)))]
          (condp = (:tag layout)
            :list (wrapped-code-element \"(\" \")\")
            :vector (wrapped-code-element \"[\" \"]\")
            :set (wrapped-code-element \"#{\" \"}\")
            :map (wrapped-code-element \"{\" \"}\")
            :fn (wrapped-code-element \"#(\" \")\")
            :meta (wrapped-code-element \"^\")
            :deref (wrapped-code-element \"@\")
            :quote (wrapped-code-element \"'\")
            :syntax-quote (wrapped-code-element \"`\")
            :unquote (wrapped-code-element \"~\")
            :unquote-splicing (wrapped-code-element \"~@\")
            [code-element-component editor-id form-id node-id layout]))))))

(defn code-box-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-layout-form-node editor-id form-id node-id])
        code-visible (subscribe [:settings :code-visible])]
    (fn [editor-id form-id node-id]
      (let [{:keys [children form-kind]} @layout]
        (log-render \"code-box\" node-id
          [:div.code-box {:class (classv (if form-kind (str \"form-kind-\" form-kind)))}
           (if @code-visible
             (let [child-id (first children)]
               [code-block-component editor-id form-id child-id]))])))))")
