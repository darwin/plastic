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

(defn code-token-component []
  (fn [node]
    (let [{:keys [decl-scope call selectable? type text shadows decl? def-name? id geometry editing?]} node
          props (merge
                  {:data-qnid id
                   :class     (classv
                                (if selectable? "selectable")
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
        (emit-token (-> text (apply-shadowing-subscripts shadows)))))))

(defn code-element-component [node]
  (let [{:keys [tag type id children]} node]
    (cond
      (= type :newline) [:br]
      (= type :whitespace) [:div.ws " "]
      (= tag :token) [code-token-component node]
      :else ^{:key id} [:div.elements
                        (for [child children]
                          ^{:key (:id child)} [code-block-component child])])))

(defn code-block [opener closer node]
  (let [{:keys [id scope selectable? depth tag scope-depth]} node
        tag-name (name tag)]
    [:div.block {:data-qnid      id
                 :class          (classv
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