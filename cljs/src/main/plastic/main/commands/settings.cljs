(ns plastic.main.commands.settings
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.commands.utils :refer [toggle-setting]]))

(defn toggle-headers [db]
  (toggle-setting db :headers-visible))

(defn toggle-code [db]
  (toggle-setting db :code-visible))

(defn toggle-docs [db]
  (toggle-setting db :docs-visible))

(defn toggle-parser-debug [db]
  (toggle-setting db :parser-debug-visible))

(defn toggle-text-input-debug [db]
  (toggle-setting db :text-input-debug-visible))

(defn toggle-text-output-debug [db]
  (toggle-setting db :text-output-debug-visible))

(defn toggle-selections-debug [db]
  (toggle-setting db :selections-debug-visible))