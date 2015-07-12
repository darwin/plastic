(ns plastic.cogs.editor.layout.structural
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [clojure.zip :as z]
            [plastic.util.zip :as zip-utils]
            [plastic.cogs.editor.layout.utils :as utils]))

(defn loc-id [loc]
  (if (utils/valid-loc? loc)
    (let [node (z/node loc)]
      (:id node))))

(defn selectables-without-docs [selectables loc]
  (let [selectable-without-docs? (fn [loc]
                                   (if-let [selectable (get selectables (loc-id loc))]
                                     (not (:doc? selectable))))]
    (selectable-without-docs? loc)))

(defn selectables-docs-only [selectables loc]
  (let [selectable-doc? (fn [loc]
                          (if-let [selectable (get selectables (loc-id loc))]
                            (:doc? selectable)))]
    (selectable-doc? loc)))

(defn structural-web-for-item [policy accum loc]
  (let [id (loc-id loc)
        zip-up (partial utils/zip-up policy)
        zip-down (partial utils/zip-down policy)
        zip-left (partial utils/zip-left policy)
        zip-right (partial utils/zip-right policy)
        up-loc (zip-up loc)
        down-loc (zip-down loc)
        left-loc (zip-left loc)
        right-loc (zip-right loc)
        record {:left  (loc-id left-loc)
                :right (loc-id right-loc)
                :up    (loc-id up-loc)
                :down  (loc-id down-loc)}]
    (conj accum [id record])))

(defn build-structural-web-for-code-tree [web policy code-locs]
  (merge web (reduce (partial structural-web-for-item policy) {} code-locs)))

(defn structural-web-for-doc [accum loc]
  (let [id (loc-id loc)
        record {:left  nil
                :right nil
                :up    nil
                :down  nil}]
    (conj accum [id record])))

(defn link-selectable-docs [web docs-locs]
  (merge web (reduce structural-web-for-doc {} docs-locs)))

(defn link-forests-to-top [web root-id]
  (merge web (map (fn [[id record]] (if (or (:up record) (= id root-id)) [id record] [id (assoc record :up root-id)])) web)))

(defn link-top-selectable [web top-id docs-locs]
  (into web [[top-id {:left  nil
                      :right nil
                      :up    nil
                      :down  (loc-id (first docs-locs))}]]))

(defn build-structural-web [top-id selectables root-loc]
  (let [all-locs (zip-utils/zip-seq root-loc)
        docs-policy (partial selectables-docs-only selectables)
        docs-locs (filter docs-policy all-locs)
        non-docs-policy (partial selectables-without-docs selectables)
        code-locs (filter non-docs-policy all-locs)]
    (-> (sorted-map)                                        ; TODO: does not need to be sorted, for debugging
      (build-structural-web-for-code-tree non-docs-policy code-locs)
      (link-selectable-docs docs-locs)
      (link-top-selectable top-id docs-locs)
      (link-forests-to-top top-id))))