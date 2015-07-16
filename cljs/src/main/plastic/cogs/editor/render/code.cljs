(ns plastic.cogs.editor.render.code
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.render.utils :refer [*editor-id* wrap-specials classv]]
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

(defn code-token-component []
  (let [selection-subscription (subscribe [:editor-selection *editor-id*])]
    (fn [node]
      (let [{:keys [decl-scope call selectable? type text shadows decl? def-name? id geometry editing?]} node
            props (merge
                    {:data-qnid id
                     :class     (classv
                                  (if selectable? "selectable")
                                  (if (and selectable? (contains? @selection-subscription id)) "selected")
                                  (if type (name type))
                                  (if call "call")
                                  (if editing? "editing")
                                  (if decl-scope (str "decl-scope decl-scope-" decl-scope))
                                  (if def-name? "def-name")
                                  (if decl? "decl"))}
                    (if geometry {:style {:transform (str "translateY(" (:top geometry) "px) translateX(" (:left geometry) "px)")}}))
            emit-token (fn [html] [:div.token props
                                   (if editing?
                                     [inline-editor-component id text (or type :symbol)]
                                     [raw-html-component html])])]
        (condp = type
          :string (emit-token (-> text (wrap-specials)))
          (emit-token (-> text (apply-shadowing-subscripts shadows))))))))

(defn break-nodes-into-lines [accum node]
  (let [new-accum (assoc accum (dec (count accum)) (conj (last accum) node))]
    (if (= (:tag node) :newline)
      (conj new-accum [])
      new-accum)))

(defn emit-code-block [node]
  ^{:key (:id node)} [code-block-component node])

(defn is-simple? [node]
  (empty? (:children node)))

(defn is-newline? [node]
  (or (nil? node) (= (:tag node) :newline)))

(defn is-double-column-line? [line]
  (and (is-simple? (first line)) (not (is-newline? (second line))) (is-newline? (nth line 2 nil))))

(defn elements-table [nodes]
  (let [lines (reduce break-nodes-into-lines [[]] nodes)]
    (if (<= (count lines) 1)
      [:div.elements
       (for [node (first lines)]
         (emit-code-block node))]
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
                              (emit-code-block (first line))]
                             [:td
                              (for [node (rest line)]
                                (emit-code-block node))]]
              ^{:key index} [:tr
                             [:td {:col-span 2}
                              (if (not= line (first lines)) [:div.indent])
                              (for [node line]
                                (emit-code-block node))]])))]])))

(defn code-element-component [node]
  (let [{:keys [tag children]} node]
    (condp = tag
      :newline [:span.newline "↵"]
      :token [code-token-component node]
      (elements-table children))))

(defn code-block [opener closer node]
  (let [{:keys [id scope selectable? depth tag scope-depth]} node
        tag-name (name tag)]
    [:div.block {:data-qnid id
                 :class     (classv
                              tag-name
                              (if selectable? "selectable")
                              (if scope (str "scope scope-" scope " scope-depth-" scope-depth))
                              (if depth (str "depth-" depth)))}
     [:div.punctuation.opener opener]
     (code-element-component node)
     [:div.punctuation.closer closer]]))

(defn code-block-component [node]
  (condp = (:tag node)
    :list (code-block "(" ")" node)
    :vector (code-block "[" "]" node)
    :set (code-block "#{" "}" node)
    :map (code-block "{" "}" node)
    :fn (code-block "#(" ")" node)
    :meta (code-block "^" "" node)
    (code-element-component node)))

(defn extract-first-child-name [node]
  (:text (first (:children node))))

(defn code-box-component []
  (fn [code-render-info]
    (let [node (first (:children code-render-info))
          name (extract-first-child-name node)]
      [:div.code-box {:class (if name (str "sexpr-" name))}
       [code-block-component node]])))