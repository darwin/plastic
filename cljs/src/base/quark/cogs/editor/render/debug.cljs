(ns quark.cogs.editor.render.debug
  (:require [quark.cogs.editor.utils :as utils])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn plaintext-debug-component [form]
  [:div.fancy
   [:div (:text form)]])

(defn parser-debug-component [parse-tree]
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