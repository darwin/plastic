(ns quark.cogs.editor.render.code
  (:require [quark.cogs.editor.render.utils :refer [raw-html wrap-specials id! classv]]
            [clojure.string :as string])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(declare code-component)

(defn visualise-shadowing [text shadows]
  (cond
    (>= shadows 2) (str text "<sub>" shadows "</sub>")
    :default text))

(defn visualize-decl [text decl?]
  (if decl?
    (str "<span class=\"decl\">" text "</span>")
    text))

(defn visualize-def-name [text def-name?]
  (if def-name?
    (str "<span class=\"def-name\">" text "</span>")
    text))

(defn visualise-doc [text doc?]
  (if doc?
    (str "<span class=\"def-doc\">" text "</span>")
    text))

(defn visualise-keyword [text]
  (string/replace text #":" "<span class=\"colon\">:</span>"))

(defn code-element-component [node]
  (let [{:keys [decl-scope tag text shadows decl? def-name? def-doc? cursor]} node]
    (cond
      (= tag :newline) [:br]
      (= tag :whitespace) [:div.token " "]
      (= tag :keyword) [:div.token.keyword (raw-html (visualise-keyword text))]
      text [:div.token (merge
                         {:class (classv
                                   (name tag)
                                   (if decl-scope (str "decl-scope " "decl-scope-" decl-scope))
                                   (if cursor "cursor"))}
                         (raw-html (-> text
                                     wrap-specials
                                     (visualise-doc def-doc?)
                                     (visualise-shadowing shadows)
                                     (visualize-decl decl?)
                                     (visualize-def-name def-name?))))]
      :else ^{:key (id!)} [:div.wrap
                           (for [child (:children node)]
                             ^{:key (id!)} [code-component child])])))

(defn wrap [open close tree]
  (let [{:keys [scope depth tag cursor]} tree]
    [:div.compound {:class (classv
                             (name tag)
                             (if cursor "cursor")
                             (if scope (str "scope " "scope-" scope))
                             (if depth (str "depth " "depth-" depth)))}
     [:div.token.punctuation.vat {:class (name tag)} open]
     (code-element-component tree)
     [:div.token.punctuation.vab {:class (name tag)} close]]))

(defn code-component [tree]
  (condp = (:tag tree)
    :list (wrap "(" ")" tree)
    :vector (wrap "[" "]" tree)
    :set (wrap "#{" "}" tree)
    :map (wrap "{" "}" tree)
    (code-element-component tree)))

(defn extract-first-child-name [node]
  (:text (first (:children node))))

(defn code-wrapper-component [form]
  (let [node (:code-tree form)
        name (extract-first-child-name node)]
    [:div.code-wrapper.noselect
     [:div.code {:class (if name (str "sexpr-" name))}
      [code-component node]]]))