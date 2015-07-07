(ns quark.cogs.editor.render.code
  (:require [quark.cogs.editor.render.utils :refer [raw-html wrap-specials classv]]
            [clojure.string :as string]
            [quark.util.helpers :as helpers])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(declare code-component)

(defn visualise-shadowing [text shadows]
  (cond
    (>= shadows 2) (str text "<span class=\"shadowed\">" shadows "</span>")
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

(defn code-token-component [node]
  (let [{:keys [decl-scope call selectable type text shadows decl? def-name? def-doc? id geometry]} node
        props (merge
                {:data-qid id
                 :class    (classv
                             (if call "call")
                             (if selectable "selectable")
                             (if decl-scope (str "decl-scope decl-scope-" decl-scope)))}
                (if geometry {:style {:transform (str "translateY(" (:top geometry) "px)" "translateX(" (:left geometry) "px)")}}))]
    (log "R! token" id)
    (cond
      (= type :keyword)
      [:div.token.keyword
       (merge
         props
         (raw-html (-> text
                     (visualise-keyword))))]

      (= type :string)
      [:div.token.string
       (merge
         props
         (raw-html (-> text
                     (wrap-specials)
                     (visualise-doc def-doc?))))]

      :else
      [:div.token
       (merge
         props
         (raw-html (-> text
                     (visualise-shadowing shadows)
                     (visualize-decl decl?)
                     (visualize-def-name def-name?))))])))

(defn code-element-component [node]
  (let [{:keys [tag type id]} node]
    (log "R! element" id)
    (cond
      (= type :newline) [:br]
      (= type :whitespace) [:div.ws " "]
      (= tag :token) [code-token-component node]
      :else ^{:key id} [:div.children
                        (for [child (:children node)]
                          ^{:key (:id child)} [code-component child])])))

(defn wrap [open close tree]
  (let [{:keys [id scope selectable depth tag]} tree]
    [:div.compound {:data-qid id
                    :class    (classv
                                (name tag)
                                (if selectable "selectable")
                                (if scope (str "scope scope-" scope))
                                (if depth (str "depth-" depth)))}
     [:div.punctuation.vat {:class (name tag)} open]
     (code-element-component tree)
     [:div.punctuation.vab {:class (name tag)} close]]))

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
  (let [node (:code form)
        name (extract-first-child-name node)]
    [:div.code-wrapper.noselect
     [:div.code {:class (if name (str "sexpr-" name))}
      [code-component node]]]))