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
                    :up    (safe-loc-id up-loc)
                    :down  (safe-loc-id down-loc)}]
        (cond-> accum
          true (assoc id record)
          (node/inner? node) (structural-web-for-spot-item id down-loc))))))

(defn link-selectable-docs [web docs-locs form-id]
  (process docs-locs web
    (fn [accum loc]
      (let [id (zip-utils/loc-id loc)
            record {:left  nil
                    :right nil
                    :up    form-id
                    :down  nil}]
        (assoc accum id record)))))

(defn build-structural-web [form-loc]
  (let [form-id (zip-utils/loc-id form-loc)
        all-locs (zip-utils/zip-seq form-loc)
        docs-locs (filter utils/is-doc? all-locs)
        code-locs (remove utils/is-doc? all-locs)]
    (-> {}
      (build-structural-web-for-code code-locs)
      (link-selectable-docs docs-locs form-id))))
