(ns plastic.worker.editor.layout.builder
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.common :refer [process]])
  (:require [plastic.util.helpers :as helpers]
            [plastic.worker.editor.layout.utils :as utils]
            [plastic.worker.editor.toolkit.id :as id]
            [meld.zip :as zip]
            [meld.node :as node]
            [clojure.string :as string]))

; -------------------------------------------------------------------------------------------------------------------

(defn linebreak-near-doc? [loc]
  (or
    (utils/linebreak-near-doc? loc zip/left)
    (utils/linebreak-near-doc? loc zip/right)))

(defn linebreak-before-standalone-comment? [loc]
  (let [right-loc (zip/right loc)]
    (if (zip/good? right-loc)
      (utils/standalone-comment? right-loc))))

(defn ignored-linebreak? [loc]
  (or
    (linebreak-near-doc? loc)
    (linebreak-before-standalone-comment? loc)))

(defn child-locs-without-comments-and-ignored-linebreaks [loc]
  (let [ignored? (fn [loc] (or (zip/comment? loc) (ignored-linebreak? loc)))]
    (remove ignored? (zip/child-locs loc))))

(defn prepare-node-text [node]
  (cond
    (node/string? node) (utils/prepare-string-for-display (node/get-source node))
    (node/keyword? node) (helpers/strip-colon (node/get-source node))
    :else (node/get-source node)))

(defn linebreak? [loc]
  (or (nil? loc) (zip/linebreak? loc)))

(defn is-simple? [loc]
  (or (nil? loc) (not (zip/compound? loc))))

(defn is-double-column-line? [line]
  (let [items (if (linebreak? (first line)) (rest line) line)]
    (if (= (count items) 2)
      (is-simple? (first items)))))

(defn break-locs-into-lines [accum loc]
  (let [accum (if (linebreak? loc) (conj accum []) accum)]
    (assoc accum (dec (count accum)) (conj (last accum) loc))))

(defn prepare-children-table [locs]
  (let [relevant-locs (remove #(or (utils/doc? %) (linebreak-near-doc? %)) locs)
        lines (reduce break-locs-into-lines [[]] relevant-locs)]
    (if (<= (count lines) 1)
      [{:oneliner? true} (cons {} (map zip/get-id (first lines)))]
      (let [first-line-is-double-column? (is-double-column-line? (first lines))]
        (cons {:multiline? true}
          (for [line lines]
            (if (is-double-column-line? line)
              (let [indent? (and (not first-line-is-double-column?) (not= line (first lines)))]
                (cons {:double-column? true :indent indent?} (map zip/get-id line)))
              (let [indent? (not= line (first lines))]
                (cons {:indent indent?} (map zip/get-id line))))))))))

(defn prepend-spot [table node-id]
  (let [spot-id (id/make-spot node-id)
        [opts & lines] table
        [hints & line] (first lines)]
    (cons opts (cons (cons hints (cons spot-id line)) (rest lines)))))

(defn process-children [loc]
  (-> loc
    (child-locs-without-comments-and-ignored-linebreaks)
    (prepare-children-table)
    (prepend-spot (zip/get-id loc))))

(defn add-code-item [accum loc]
  (let [node (zip/get-node loc)
        node-id (node/get-id node)
        inner? (node/compound? node)
        tag (node/get-tag node)
        type (node/get-type node)
        prev-loc (zip/prev loc)
        after-nl? (and (zip/good? prev-loc) (linebreak? prev-loc) (not (ignored-linebreak? prev-loc)))]
    (-> accum
      (update :code #(conj % node-id))
      (assoc-in [:data node-id]
        (cond-> {:id            node-id
                 :section       :code
                 :tag           tag
                 :type          type
                 :line          (:line accum)
                 :spatial-index (:line accum)
                 :selectable?   true
                 :editable?     (not (= type :linebreak))}
          after-nl? (assoc :after-nl true)
          inner? (assoc :children (process-children loc))
          (not inner?) (assoc :text (prepare-node-text node)))))))

(defn add-spot-item [accum loc]
  (let [node-id (zip/get-id loc)
        spot-id (id/make-spot node-id)
        line (:line accum)]
    (-> accum
      (assoc-in [:data spot-id]
        {:id            spot-id
         :type          :spot
         :tag           :spot
         :section       :code
         :line          line
         :spatial-index line
         :selectable?   true
         :editable?     true
         :text          ""}))))

(defn add-doc-item [accum loc]
  (let [node-id (zip/get-id loc)
        text (zip/get-source loc)]
    (-> accum
      (update :docs #(conj % node-id))
      (assoc-in [:data node-id]
        {:id            node-id
         :type          :doc
         :tag           :doc
         :section       :docs
         :selectable?   true
         :editable?     true
         :line          0
         :spatial-index (count (:docs accum))
         :text          (utils/prepare-string-for-display text)}))))

(defn add-comment-item [accum loc]
  (let [node-id (zip/get-id loc)
        line (:line accum)]
    (-> accum
      (update :comments #(conj % node-id))
      (assoc-in [:data node-id]
        {:id            node-id
         :type          :comment
         :tag           :comment
         :section       :comments
         :line          line
         :spatial-index (count (:comments accum))
         :selectable?   true
         :editable?     true
         :text          (zip/get-content loc)}))))

(defn lookup-def-arities [accum loc]
  (let [node-id (zip/get-id loc)]
    (if-let [arities (utils/lookup-arities loc)]
      (assoc-in accum [:data node-id :arities] arities)
      accum)))

(defn add-def-name [accum loc]
  (let [node-id (zip/get-id loc)]
    (-> accum
      (add-code-item loc)
      (lookup-def-arities loc)
      (update :headers #(conj % node-id)))))

(defn build-unit-layout [accum loc stop-id]
  (if (= (zip/get-id loc) stop-id)
    accum
    (let [next-loc (zip/next loc)
          ignored? (ignored-linebreak? loc)]
      (if ignored?
        (recur accum next-loc stop-id)
        (let [add-spot-item-if-needed (fn [accum]
                                        (if (zip/compound? loc)
                                          (add-spot-item accum loc)
                                          accum))
              do-layout-item (fn [accum]
                               (cond
                                 (utils/doc? loc) (add-doc-item accum loc)
                                 (zip/comment? loc) (add-comment-item accum loc)
                                 (utils/def-name? loc) (add-def-name accum loc)
                                 :else (add-code-item accum loc)))
              detect-new-lines (fn [accum]
                                 (if (linebreak? loc)
                                   (update accum :line inc) accum))
              new-accum (-> accum
                          detect-new-lines
                          do-layout-item
                          add-spot-item-if-needed)]
          (recur new-accum next-loc stop-id))))))

(defn get-first-leaf-expr [loc]
  (let [bottom-loc (zip/next loc)
        right-loc (zip/right loc)]
    (if (and (zip/good? bottom-loc) (not= (zip/get-id right-loc) (zip/get-id bottom-loc)))
      (node/get-source (zip/get-node bottom-loc)))))

(defn count-comment-lines [comment-layout]
  (count (string/split (:text comment-layout) #"\n")))

(defn compute-comments-metrics [data comment-ids]
  (first (process comment-ids [[] 0]
           (fn [[metrics end] comment-id]
             (let [comment-layout (get data comment-id)
                   preferred-start (:line comment-layout)
                   size (count-comment-lines comment-layout)
                   diff (- preferred-start end)
                   spacing (if (neg? diff) 0 diff)
                   new-start (+ end spacing)
                   new-end (+ new-start size)]
               [(conj metrics spacing) new-end])))))

(defn build-layout [unit-loc]
  {:pre [(zip/good? unit-loc)]}
  (let [initial {:data {} :code [] :docs [] :headers [] :comments [] :line 0}
        stop-id (zip/get-id (zip/right unit-loc))
        first-loc (zip/down unit-loc)
        {:keys [data code docs headers comments _line]} (build-unit-layout initial first-loc stop-id)
        unit-id (zip/get-id unit-loc)
        unit-type (name (zip/get-tag first-loc))
        unit-kind (get-first-leaf-expr first-loc)
        has-code? (not (empty? code))
        has-docs? (not (empty? docs))
        has-headers? (not (empty? headers))
        has-comments? (not (empty? comments))
        code-id (id/make unit-id :code)
        docs-id (id/make unit-id :docs)
        comments-id (id/make unit-id :comments)
        headers-id (id/make unit-id :headers)]
    (cond-> data
      has-headers? (assoc headers-id {:tag      :headers
                                      :id       headers-id
                                      :children headers})
      has-comments? (assoc comments-id {:tag      :comments
                                        :id       comments-id
                                        :children comments
                                        :metrics  (compute-comments-metrics data comments)})
      has-docs? (assoc docs-id {:tag      :docs
                                :id       docs-id
                                :children docs})
      has-code? (assoc code-id {:tag      :code
                                :id       code-id
                                :children [(first code)]})
      true (assoc unit-id {:tag       :unit
                           :id        unit-id
                           :unit-type unit-type
                           :unit-kind unit-kind
                           :sections  {:headers  (if has-headers? headers-id)
                                       :docs     (if has-docs? docs-id)
                                       :code     (if has-code? code-id)
                                       :comments (if has-comments? comments-id)}}))))
