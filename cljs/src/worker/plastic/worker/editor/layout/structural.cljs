(ns plastic.worker.editor.layout.structural
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.common :refer [process]])
  (:require [clojure.zip :as z]
            [plastic.util.zip :as zip-utils]
            [plastic.worker.editor.layout.utils :as utils]
            [rewrite-clj.node :as node]
            [plastic.worker.editor.toolkit.id :as id]
            [rewrite-clj.zip :as zip]))

(defn safe-loc-id [loc]
  (if (zip-utils/valid-loc? loc)
    (zip-utils/loc-id loc)))

(defn safe-make-spot-id [id]
  (if id
    (id/make-spot id)))

(defn structure? [loc]
  (let [node (z/node loc)]
    (not (or (node/whitespace? node) (node/comment? node)))))

(def zip-up (partial zip-utils/zip-up structure?))
(def zip-down (partial zip-utils/zip-down structure?))
(def zip-left (partial zip-utils/zip-left structure?))
(def zip-right (partial zip-utils/zip-right structure?))

(defn structural-web-for-spot-item [accum id loc]
  (let [spot-id (id/make-spot id)]
    (assoc accum spot-id {:left  nil
                          :right (safe-loc-id loc)
                          :up    id
                          :down  nil})))

(defn ignore-form [loc]
  (if-not (zip-utils/form? loc) loc))

(defn build-structural-web-for-code [web code-locs]
  (process code-locs web
    (fn [accum loc]
      (let [node (zip/node loc)
            id (:id node)
            up-loc (zip-up loc)
            down-loc (zip-down loc)
            left-loc (zip-left loc)
            right-loc (zip-right loc)
            record {:left  (or (safe-loc-id left-loc) (safe-make-spot-id (safe-loc-id up-loc)))
                    :right (safe-loc-id right-loc)
                    :up    (safe-loc-id (ignore-form up-loc))
                    :down  (safe-loc-id down-loc)}]
        (cond-> accum
          true (assoc id record)
          (node/inner? node) (structural-web-for-spot-item id down-loc))))))

(defn build-structural-web-for-comments [web comments-layout]
  (let [root-id (:id comments-layout)
        ids (vec (:children comments-layout))]
    (process (map-indexed (fn [i v] [i v]) ids) web
      (fn [accum [index comment-id]]
        (let [record {:left  (nth ids (dec index) nil)
                      :right (nth ids (inc index) nil)
                      :up    root-id
                      :down  nil}]
          (assoc accum comment-id record))))))

(defn build-structural-web [form-loc layout]
  (let [form-id (zip-utils/loc-id form-loc)
        all-locs (zip-utils/zip-seq form-loc)
        comments-id (id/make form-id :comments)
        comments-layout (get layout comments-id)
        code-locs (remove utils/is-doc? all-locs)]
    (cond-> {}
      true (build-structural-web-for-code code-locs)
      comments-layout (build-structural-web-for-comments comments-layout))))
