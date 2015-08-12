(ns plastic.main.editor.toolkit.id
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [clojure.string :as string]))

; ids can be simple numbers pointing to parsed node-ids
; or composed id-tag strings, where tag can be a distinction for virtual nodes not necesarilly present in the parsed tree

(defn valid? [id]
  (or (string? id) (and (number? id) (pos? id))))

(defn make [node-id keyword]
  (str node-id "-" (name keyword)))

(defn parts [id]
  (if (number? id)
    [id nil]
    (let [[id-part key-part] (string/split id #"-")]
      [(int id-part) (keyword key-part)])))

(defn id-part [id]
  (first (parts id)))

(defn key-part [id]
  (second (parts id)))

(defn is? [id key]
  (= key (key-part id)))

(defn make-spot [node-id]
  (make node-id :spot))

(defn spot? [node-id]
  (is? node-id :spot))