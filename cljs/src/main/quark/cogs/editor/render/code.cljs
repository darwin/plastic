(ns quark.cogs.editor.render.code
  (:require [quark.cogs.editor.render.utils :refer [raw-html wrap-specials classv]]
            [clojure.string :as string]
            [quark.onion.api :refer [$]]
            [quark.util.helpers :as helpers]
            [quark.cogs.editor.utils :as utils]
            [reagent.core :as reagent]
            [quark.onion.core :as onion])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(declare code-component)

(defn visualise-shadowing [text shadows]
  (if (>= shadows 2)
    (str text (utils/wrap-in-span shadows "shadowed"))
    text))

(defn visualize-decl [text decl?]
  (utils/wrap-in-span decl? text "decl"))

(defn visualize-def-name [text def-name?]
  (utils/wrap-in-span def-name? text "def-name"))

(defn visualise-doc [text doc?]
  (utils/wrap-in-span doc? text "def-doc"))

(defn visualise-keyword [text]
  (string/replace text #":" "<span class=\"colon\">:</span>"))

(defn inline-editor-present? [$dom-node]
  (aget (.children $dom-node ".editor") 0))

(defn activate-inline-editor [$dom-node]
  (when-not (inline-editor-present? $dom-node)
    (let [editor-dom-node (aget (.closest $dom-node ".quark-editor") 0)
          _ (assert editor-dom-node)
          editor-id (.getAttribute editor-dom-node "data-qid")
          _ (assert editor-id)
          {:keys [editor view]} (onion/prepare-editor-instance (int editor-id))
          $view ($ view)]
      (log "activate inline editor")
      (.setUpdatedSynchronously view true)
      (.setText editor (.data $dom-node "text"))
      (.selectAll editor)
      (.setUpdatedSynchronously view false)
      (.append $dom-node view)
      ;(.show $view)
      (.focus view))))

(defn deactivate-inline-editor [$dom-node]
  (when-let [inline-editor-view (inline-editor-present? $dom-node)]
    (log "deactivate inline editor")
    (let [editor-dom-node (aget (.closest $dom-node ".quark-editor-view") 0)
          _ (assert editor-dom-node)]
      (log "focus" editor-dom-node)
      (.focus editor-dom-node))))

(defn inline-editor-action [action react-component]
  (let [dom-node (.getDOMNode react-component)              ; TODO: deprecated!
        _ (assert dom-node)
        $dom-node ($ dom-node)
        skelet-dom-node (aget (.closest $dom-node ".form-skelet") 0)]
    (if skelet-dom-node
      (condp = action
        :activate (activate-inline-editor $dom-node)
        :deactivate (deactivate-inline-editor $dom-node)))
    nil))

(defn inline-editor-scaffold [render-fn]
  (reagent/create-class
    {:component-did-mount    (partial inline-editor-action :activate)
     :component-did-update   (partial inline-editor-action :activate)
     :component-will-unmount (partial inline-editor-action :deactivate)
     :reagent-render         render-fn}))

(defn inline-editor-component []
  (inline-editor-scaffold
    (fn [text]
      [:div.inline-editor
       {:data-text text}])))

(defn raw-html-component [html]
  [:div.raw (raw-html html)])

(defn code-token-component []
  (fn [node]
    (let [{:keys [decl-scope call selectable type text shadows decl? def-name? def-doc? id geometry editing?]} node
          props (merge
                  {:data-qid id
                   :class    (classv
                               (if selectable "selectable")
                               (if type (name type))
                               (if call "call")
                               (if editing? "editing")
                               (if decl-scope (str "decl-scope decl-scope-" decl-scope)))}
                  (if geometry {:style {:transform (str "translateY(" (:top geometry) "px) translateX(" (:left geometry) "px)")}}))
          emit-token (fn [html] [:div.token props
                                 (if editing?
                                   [inline-editor-component text]
                                   [raw-html-component html])])]
      (condp = type
        :keyword (emit-token (-> text (visualise-keyword)))
        :string (emit-token (-> text (wrap-specials) (visualise-doc def-doc?)))
        (emit-token (-> text (visualise-shadowing shadows) (visualize-decl decl?) (visualize-def-name def-name?)))))))

(defn code-element-component [node]
  (let [{:keys [tag type id]} node]
    (cond
      (= type :newline) [:br]
      (= type :whitespace) [:div.ws " "]
      (= tag :token) [code-token-component node]
      :else ^{:key id} [:div.children
                        (for [child (:children node)]
                          ^{:key (:id child)} [code-component child])])))

(defn wrap [open close node]
  (let [{:keys [id scope selectable depth tag]} node
        tag-name (name tag)]
    [:div.compound {:data-qid id
                    :class    (classv
                                tag-name
                                (if selectable "selectable")
                                (if scope (str "scope scope-" scope))
                                (if depth (str "depth-" depth)))}
     [:div.punctuation.vat {:class tag-name} open]
     (code-element-component node)
     [:div.punctuation.vab {:class tag-name} close]]))

(defn code-component [node]
  (condp = (:tag node)
    :list (wrap "(" ")" node)
    :vector (wrap "[" "]" node)
    :set (wrap "#{" "}" node)
    :map (wrap "{" "}" node)
    :fn (wrap "#(" ")" node)
    (code-element-component node)))

(defn extract-first-child-name [node]
  (:text (first (:children node))))

(defn code-wrapper-component [form]
  (let [node (:code form)
        name (extract-first-child-name node)]
    [:div.code-wrapper
     [:div.code {:class (if name (str "sexpr-" name))}
      [code-component node]]]))