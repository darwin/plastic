(ns quark.cogs.editor.render.debug
  (:require [quark.util.helpers :as helpers])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn plaintext-debug-component [form]
  [:div.fancy
   [:div (:text form)]])

(defn parser-debug-component [parse-tree]
  [:div.state
   [:div (helpers/nice-print parse-tree)]])

(defn code-debug-component [form]
  [:div.code-debug
   [:div (helpers/nice-print (:code form))]])

(defn docs-debug-component [form]
  [:div.docs-debug
   [:div (helpers/nice-print (:docs form))]])

(defn headers-debug-component [form]
  [:div.headers-debug
   [:div (helpers/nice-print (:headers form))]])

(defn debug-component [form]
  [:div.debug
   [:div (helpers/nice-print (:debug form))]])