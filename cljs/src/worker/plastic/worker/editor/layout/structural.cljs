(ns plastic.worker.editor.layout.structural
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.common :refer [process]])
  (:require [plastic.worker.editor.layout.utils :as utils]
            [plastic.worker.editor.toolkit.id :as id]
            [meld.zip :as zip]))

(defn safe-loc-id [loc]
  (if (zip/good? loc)
    (zip/id loc)))

(defn safe-make-spot-id [id layout]
  (if id
    (let [spot-id (id/make-spot id)]
      (if (get layout spot-id)
        spot-id))))

(defn structural-web-for-spot-item [accum id loc]
  (let [spot-id (id/make-spot id)]
    (assoc accum spot-id {:left  nil
                          :right (safe-loc-id loc)
                          :up    id
                          :down  nil})))

(defn build-structural-web-for-code [web code-locs layout]
  (process code-locs web
    (fn [accum loc]
      (let [id (zip/id loc)
            up-loc (zip/up loc)
            down-loc (zip/down loc)
            left-loc (zip/left loc)
            right-loc (zip/right loc)
            record {:left  (or (safe-loc-id left-loc) (safe-make-spot-id (safe-loc-id up-loc) layout))
                    :right (safe-loc-id right-loc)
                    :up    (safe-loc-id up-loc)
                    :down  (safe-loc-id down-loc)}]
        (cond-> accum
          true (assoc id record)
          (zip/compound? loc) (structural-web-for-spot-item id down-loc))))))

(defn build-structural-web-for-comments [web comments-layout]
  (let [ids (vec (:children comments-layout))]
    (process (map-indexed (fn [i v] [i v]) ids) web
      (fn [accum [index comment-id]]
        (let [record {:left  (nth ids (dec index) nil)
                      :right (nth ids (inc index) nil)
                      :up    nil                                                                                      ; later we could group all under a virtual comments node
                      :down  nil}]
          (assoc accum comment-id record))))))

(defn build-structural-web [unit-loc layout]
  (let [unit-id (zip/id unit-loc)
        all-locs (zip/descendant-locs unit-loc)
        comments-id (id/make unit-id :comments)
        comments-layout (get layout comments-id)
        code-locs (remove utils/doc? all-locs)]
    (cond-> {}
      true (build-structural-web-for-code code-locs layout)
      comments-layout (build-structural-web-for-comments comments-layout))))
