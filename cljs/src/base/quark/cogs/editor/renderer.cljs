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

(defn structure-component [form]
  [:div.structure-state
   [:div (utils/print-node (:structure form))]])

(defn soup-component [form]
  [:div.soup
   (for [item (:soup form)]
     (if (= (:tag item) :newline)
       ^{:key (id!)} [:br]
       ^{:key (id!)} [:div.soup-item
                      (:string item)]))])

(declare structural-component)

(defn wrap-specials [s]
  (-> s
    (string/replace #"↵" "<i>↵</i>")
    (string/replace #"␣" "<i>.</i>")
    (string/replace #"⇥" "<i>»</i>")))

(defn inner-structural-component [tree]
  (cond
    (= (:tag tree) :newline) [:br]
    (= (:tag tree) :whitespace) [:div.token " "]
    (:text tree) [:div.token {:class                   (:tag tree)
                              :dangerouslySetInnerHTML {:__html (wrap-specials (:text tree))}}]
    :else ^{:key (id!)} [:div.wrap
                         (for [child (:children tree)]
                           ^{:key (id!)} [structural-component child])]))

(defn wrap [open close tree]
  (let [colors (get colors-by-type (:tag tree))]
    [:div.compound {:class (:tag tree)
                    :style {:background-color (nth bg-colors (inc (:depth tree)))
                            :border-color     (nth bg-colors (:depth tree))}}
     [:div.token.punctuation.vat open]
     (inner-structural-component tree)
     [:div.token.punctuation.vab close]]))

(defn structural-component [tree]
  (condp = (:tag tree)
    :list (wrap "(" ")" tree)
    :vector (wrap "[" "]" tree)
    :set (wrap "#{" "}" tree)
    :map (wrap "{" "}" tree)
    (inner-structural-component tree)))

(defn structural-component-wrapper [tree]
  [:div.structural
   [structural-component tree]])

(defn forms-component [forms]
  [:div.quark-form-group
   (for [form forms]
     ^{:key (id!)} [:div.quark-form
                    [plain-text-compoent form]
                    [structural-component-wrapper (:structure form)]
                    [structure-component form]
                    ;[soup-component form]
                    ])])

(defn editor-root-component [editor-id]
  (let [state (subscribe [:editor-render-state editor-id])]
    (fn []
      (let [forms (:forms @state)
            parse-tree (:parse-tree @state)]
        ^{:key (id!)} [:div.quark-editor-root
                       [forms-component forms]
                       [state-component parse-tree]
                       ]))))

(defn mount-editor [element editor-id]
  (let [editor (partial editor-root-component editor-id)]
    (reagent/render [editor] element)))