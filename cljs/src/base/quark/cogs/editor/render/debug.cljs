(ns quark.cogs.editor.render.debug
  (:require [quark.cogs.editor.utils :as utils]
            [quark.util.helpers :as helpers])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn plaintext-debug-component [form]
  [:div.fancy
   [:div (:text form)]])

(defn parser-debug-component [parse-tree]
  [:div.state
   [:div (helpers/print parse-tree)]])

(defn code-debug-component [form]
  [:div.code-debug
   [:div (helpers/print (:code-tree form))]])

(defn docs-debug-component [form]
  [:div.docs-debug
   [:div (helpers/print (:docs-tree form))]])

(defn debug-component [form]
  [:div.debug
   [:div (helpers/print (:debug form))]])