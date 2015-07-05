(ns quark.cogs.editor.renderer
  (:require [reagent.core :as reagent]
            [quark.frame.core :refer [register-sub subscribe]]
            [quark.frame.middleware :refer [path]]
            [inkspot.color :as color]
            [inkspot.color-chart :as cc]
            [phalanges.core :as phalanges]
            [quark.cogs.editor.utils :as utils]
            [clojure.string :as string])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [reagent.ratom :refer [reaction]]))

(defonce ^:dynamic id 0)

(def bg-colors
  (cycle (cc/gradient "#111111" "#aaaaaa" 20)))

(def colors-by-type
  {:list   (cycle (cc/gradient "#223322" "#99aa99" 8))
   :vector (cycle (cc/gradient "#222233" "#9999aa" 8))
   :map    (cycle (cc/gradient "#332222" "#aa9999" 8))
   :set    (cycle (cc/gradient "#333322" "#aaaa99" 8))})

(log (take 11 bg-colors))

(defonce ^:dynamic cur-bg-color 0)

(defn next-bg-color []
  (set! cur-bg-color (inc cur-bg-color))
  (nth bg-colors cur-bg-color))

(defn id! []
  (set! id (inc id))
  id)

(defn plain-text-compoent [form]
  [:div.fancy
   [:div (:text form)]])

(defn state-component [parse-tree]
  [:div.state
   [:div (utils/print-node parse-tree)]])

(defn code-debug-component [form]
  [:div.code-debug
   [:div (utils/print-node (:code-tree form))]])

(defn docs-debug-component [form]
  [:div.docs-debug
   [:div (utils/print-node (:docs-tree form))]])

(defn debug-component [form]
  [:div.debug
   [:div (utils/print-node (:debug form))]])

(defn soup-component [form]
  [:div.soup
   (for [item (:soup form)]
     (if (= (:tag item) :newline)
       ^{:key (id!)} [:br]
       ^{:key (id!)} [:div.soup-item
                      (:string item)]))])

(declare code-component)

(defn wrap-specials [s]
  (-> s
    (string/replace #"\\\"" "\"")
    (string/replace #"↵" "<i>↵</i>")
    (string/replace #"␣" "<i>.</i>")
    (string/replace #"⇥" "<i>»</i>")))

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

(defn raw-html [html]
  {:dangerouslySetInnerHTML {:__html html}})

(defn classv [& v]
  (string/join " " (filter (complement nil?) v)))

(defn inner-code-component [node]
  (let [{:keys [decl-scope tag text shadows decl? def-name? def-doc?]} node]
    (cond
      (= tag :newline) [:br]
      (= tag :whitespace) [:div.token " "]
      (= tag :keyword) [:div.token.keyword (raw-html (visualise-keyword text))]
      text [:div.token (merge
                         {:class (classv (name tag) (if decl-scope (str "decl-scope " "decl-scope-" decl-scope)))}
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
  (let [{:keys [scope depth tag]} tree]
    [:div.compound {:class (classv
                             (name tag)
                             (if scope (str "scope " "scope-" scope))
                             (if depth (str "depth " "depth-" depth)))}
     [:div.token.punctuation.vat {:class (name tag)} open]
     (inner-code-component tree)
     [:div.token.punctuation.vab {:class (name tag)} close]]))

(defn code-component [tree]
  (condp = (:tag tree)
    :list (wrap "(" ")" tree)
    :vector (wrap "[" "]" tree)
    :set (wrap "#{" "}" tree)
    :map (wrap "{" "}" tree)
    (inner-code-component tree)))

(defn doc-component [doc-info]
  (let [{:keys [name doc]} doc-info]
    [:div.doc
     (if name [:div.name name])
     (if doc [:div.docstring (raw-html (wrap-specials doc))])]))

(defn docs-component [doc-info-list]
  [:div.docs-group
   (for [doc-info doc-info-list]
     (doc-component doc-info))])

(defn code-component-wrapper [form]
  [:div.code
   [code-component (:code-tree form)]])

(defn docs-component-wrapper [form]
  [:div.docs
   [docs-component (:docs-tree form)]])

(defn forms-component [forms]
  [:table.form-group
   (for [form forms]
     ^{:key (id!)} [:tr.form
                    ;[plain-text-compoent form]
                    [:td.docs-cell
                     [docs-component-wrapper form]]
                    [:td.code-cell
                     [code-component-wrapper form]]

                    ;[docs-debug-component form]
                    ;[code-debug-component form]
                    ;[debug-component form]
                    ;[soup-component form]
                    ])])

(defn editor-root-component [editor-id]
  (let [state (subscribe [:editor-render-state editor-id])]
    (fn []
      (let [forms (:forms @state)
            parse-tree (:parse-tree @state)]
        ^{:key (id!)} [:div.quark-editor-root
                       [forms-component forms]
                       ;[state-component parse-tree]
                       ]))))

(defn mount-editor [element editor-id]
  (let [editor (partial editor-root-component editor-id)]
    (reagent/render [editor] element)))