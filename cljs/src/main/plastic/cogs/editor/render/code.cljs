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
        analysis (subscribe [:editor-form-node-analysis editor-id form-id node-id])]
    (fn [node]
      (let [{:keys [selectable? type text id]} node
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

(defn break-nodes-into-lines [accum node]
  (let [new-accum (assoc accum (dec (count accum)) (conj (last accum) node))]
    (if (= (:tag node) :newline)
      (conj new-accum [])
      new-accum)))

(defn emit-code-block [editor-id form-id node]
  ^{:key (:id node)} [code-block-component editor-id form-id node])

(defn is-simple? [node]
  (empty? (:children node)))

(defn is-newline? [node]
  (or (nil? node) (= (:tag node) :newline)))

(defn is-double-column-line? [line]
  (and (is-simple? (first line)) (not (is-newline? (second line))) (is-newline? (nth line 2 nil))))

(defn elements-table [emit nodes]
  (let [lines (reduce break-nodes-into-lines [[]] nodes)]
    (if (<= (count lines) 1)
      [:div.elements
       (for [node (first lines)]
         (emit node))]
      [:table.elements
       [:tbody
        (let [first-line-is-double-column? (is-double-column-line? (first lines))]
          (for [[index line] (map-indexed (fn [i l] [i l]) lines)]
            (if (is-double-column-line? line)
              ^{:key index} [:tr
                             [:td
                              (if (and
                                    (not first-line-is-double-column?)
                                    (not= line (first lines)))
                                [:div.indent])
                              (emit (first line))]
                             [:td
                              (for [node (rest line)]
                                (emit node))]]
              ^{:key index} [:tr
                             [:td {:col-span 2}
                              (if (not= line (first lines)) [:div.indent])
                              (for [node line]
                                (emit node))]])))]])))

(defn code-element-component []
  (fn [editor-id form-id node]
    (let [{:keys [tag children]} node]
      (condp = tag
        :newline [:span.newline "↵"]
        :token [(code-token-component editor-id form-id (:id node)) node]
        (elements-table (partial emit-code-block editor-id form-id) children)))))

(defn wrapped-code-block-component [editor-id form-id node opener closer]
  (let [node-id (:id node)
        selected? (subscribe [:editor-selection-node editor-id node-id])
        cursor? (subscribe [:editor-cursor-node editor-id node-id])
        analysis (subscribe [:editor-form-node-analysis editor-id form-id node-id])]
    (fn []
      (let [{:keys [id selectable? depth tag]} node
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
         [code-element-component editor-id form-id node]
         [:div.punctuation.closer closer]]))))

(defn code-block-component []
  (fn [editor-id form-id node]
    (log "R! code-block" (:id node))
    (let [wrapped-code-block (partial wrapped-code-block-component editor-id form-id node)]
      (condp = (:tag node)
        :list [wrapped-code-block "(" ")"]
        :vector [wrapped-code-block "[" "]"]
        :set [wrapped-code-block "#{" "}"]
        :map [wrapped-code-block "{" "}"]
        :fn [wrapped-code-block "#(" ")"]
        :meta [wrapped-code-block "^" ""]
        [code-element-component editor-id form-id node]))))

(defn extract-first-child-name [node]
  (:text (first (:children node))))

(defn code-box-component []
  (fn [editor-id form-id code-render-info]
    (log "R! code-box")
    (let [node (first (:children code-render-info))
          name (extract-first-child-name node)]
      [:div.code-box {:class (if name (str "sexpr-" name))}
       [code-block-component editor-id form-id node]])))