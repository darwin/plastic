(ns plastic.worker.editor.layout.builder
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.common :refer [process]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [clojure.zip :as z]
            [plastic.util.zip :as zip-utils :refer [valid-loc? loc-id]]
            [plastic.util.helpers :as helpers]
            [plastic.worker.editor.layout.utils :as utils]
            [plastic.worker.editor.toolkit.id :as id]))

(defn strip-whitespaces-but-keep-linebreaks-policy [loc]
  (let [node (z/node loc)]
    (or (node/linebreak? node) (not (node/whitespace? node)))))

(def zip-down (partial zip-utils/zip-down strip-whitespaces-but-keep-linebreaks-policy))
(def zip-right (partial zip-utils/zip-right strip-whitespaces-but-keep-linebreaks-policy))
(def zip-left (partial zip-utils/zip-left strip-whitespaces-but-keep-linebreaks-policy))
(def zip-next (partial zip-utils/zip-next strip-whitespaces-but-keep-linebreaks-policy))
(def zip-prev (partial zip-utils/zip-prev strip-whitespaces-but-keep-linebreaks-policy))

(defn collect-all-right [loc]
  (take-while valid-loc? (iterate zip-right loc)))

(defn child-locs [loc]
  (collect-all-right (zip-down loc)))

(defn prepare-node-text [node]
  (cond
    (utils/string-node? node) (utils/prepare-string-for-display (node/string node))
    (utils/keyword-node? node) (helpers/strip-colon (node/string node))
    :else (node/string node)))

(defn is-linebreak? [loc]
  (or (nil? loc) (node/linebreak? (z/node loc))))

(defn is-simple? [loc]
  (if (nil? loc)
    true
    (not (node/inner? (z/node loc)))))

(defn is-double-column-line? [line]
  (let [items (if (is-linebreak? (first line)) (rest line) line)]
    (if (= (count items) 2)
      (is-simple? (first items)))))

(defn linebreak-near-doc? [loc]
  (or
    (utils/is-linebreak-near-doc? loc zip-left)
    (utils/is-linebreak-near-doc? loc zip-right)))

(defn linebreak-near-comment? [loc]
  (utils/is-linebreak-near-comment? loc zip-right))

(defn ignored-linebreak? [loc]
  (or
    (linebreak-near-doc? loc)
    (linebreak-near-comment? loc)))

(defn break-locs-into-lines [accum loc]
  (let [accum (if (is-linebreak? loc) (conj accum []) accum)]
    (assoc accum (dec (count accum)) (conj (last accum) loc))))

(defn prepare-children-table [locs]
  (let [relevant-locs (remove #(or (utils/is-doc? %) (linebreak-near-doc? %)) locs)
        lines (reduce break-locs-into-lines [[]] relevant-locs)]
    (if (<= (count lines) 1)
      [{:oneliner? true} (cons {} (map loc-id (first lines)))]
      (let [first-line-is-double-column? (is-double-column-line? (first lines))]
        (cons {:multiline? true}
          (for [line lines]
            (if (is-double-column-line? line)
              (let [indent? (and (not first-line-is-double-column?) (not= line (first lines)))]
                (cons {:double-column? true :indent indent?} (map loc-id line)))
              (let [indent? (not= line (first lines))]
                (cons {:indent indent?} (map loc-id line))))))))))

(defn prepend-spot [table node-id]
  (let [spot-id (id/make-spot node-id)
        [opts & lines] table
        [hints & line] (first lines)]
    (cons opts (cons (cons hints (cons spot-id line)) (rest lines)))))

(defn process-children [loc]
  (-> loc
    (child-locs)
    (prepare-children-table)
    (prepend-spot (loc-id loc))))

(defn add-code-item [accum loc]
  (let [node (zip/node loc)
        node-id (:id node)
        inner? (node/inner? node)
        linebreak? (is-linebreak? loc)
        prev-loc (zip-prev loc)
        after-nl? (and (valid-loc? prev-loc) (is-linebreak? prev-loc) (not (ignored-linebreak? prev-loc)))]
    (-> accum
      (update :code #(conj % node-id))
      (assoc-in [:data node-id]
        (cond-> {:id            node-id
                 :line          (:line accum)
                 :spatial-index (:line accum)
                 :tag           (node/tag node)
                 :section       :code
                 :type          :symbol}
          inner? (assoc :children (process-children loc))
          after-nl? (assoc :after-nl true)
          (not inner?) (assoc :text (prepare-node-text node))
          (utils/is-selectable? (node/tag node)) (assoc :selectable? true)
          linebreak? (assoc :type :linebreak)
          linebreak? (assoc :tag :token)
          (utils/string-node? node) (assoc :type :string)
          (utils/keyword-node? node) (assoc :type :keyword))))))

(defn add-spot-item [accum loc]
  (let [node (zip/node loc)
        node-id (:id node)
        spot-id (id/make-spot node-id)]
    (assoc-in accum [:data spot-id] {:id            spot-id
                                     :tag           :token
                                     :type          :spot
                                     :section       :code
                                     :line          (:line accum)
                                     :spatial-index (:line accum)
                                     :selectable?   true
                                     :text          ""})))

(defn add-doc-item [accum loc]
  (let [node (z/node loc)
        node-id (:id node)
        text (node/string node)]
    (-> accum
      (update :docs #(conj % node-id))
      (assoc-in [:data node-id]
        {:id            node-id
         :tag           :token
         :type          :doc
         :section       :docs
         :selectable?   true
         :line          0
         :spatial-index (count (:docs accum))
         :text          (utils/prepare-string-for-display text)}))))

(defn add-comment-item [accum loc]
  (let [combined-comment (utils/combine-comments loc)
        node-id (:id combined-comment)]
    (-> accum
      (update :comments #(conj % node-id))
      (assoc-in [:data node-id] (merge combined-comment
                                  {:tag           :comment
                                   :type          :comment
                                   :section       :comments
                                   :line          (:line accum)
                                   :spatial-index (count (:comments accum))
                                   :selectable?   true})))))

(defn lookup-def-arities [accum loc]
  (let [node (zip/node loc)
        node-id (:id node)]
    (if-let [arities (utils/lookup-arities loc)]
      (assoc-in accum [:data node-id :arities] arities)
      accum)))

(defn add-def-name [accum loc]
  (let [node (zip/node loc)
        node-id (:id node)]
    (-> accum
      (add-code-item loc)
      (lookup-def-arities loc)
      (update :headers #(conj % node-id)))))

(defn build-form-layout [accum loc]
  (if-not (valid-loc? loc)
    accum
    (let [slurp-loc (if (utils/is-comment? loc)
                      (last (utils/slurp-comments loc))
                      loc)
          next-loc (zip-next slurp-loc)
          ignored? (ignored-linebreak? loc)]
      (if ignored?
        (recur accum next-loc)
        (let [layout-item (fn [accum]
                            (cond
                              (utils/is-doc? loc) (add-doc-item accum loc)
                              (utils/is-comment? loc) (add-comment-item accum loc)
                              (utils/is-def-name? loc) (add-def-name accum loc)
                              :else (cond-> accum
                                      (utils/inner? loc) (add-spot-item loc)
                                      true (add-code-item loc))))
              detect-new-lines (fn [accum]
                                 (if (is-linebreak? loc)
                                   (update accum :line inc) accum))
              new-accum (-> accum
                          detect-new-lines
                          layout-item)]
          (recur new-accum next-loc))))))

(defn get-first-leaf-expr [loc]
  (let [bottom-loc (last (take-while valid-loc? (iterate zip-down loc)))]
    (if (valid-loc? bottom-loc)
      (node/string (z/node bottom-loc)))))

(defn compute-comments-metrics [data comment-ids]
  (first (process comment-ids [[] 0]
           (fn [[metrics end] comment-id]
             (let [comment-info (get data comment-id)
                   preferred-start (:line comment-info)
                   size (:lines comment-info)
                   diff (- preferred-start end)
                   spacing (if (neg? diff) 0 diff)
                   new-start (+ end spacing)
                   new-end (+ new-start size)]
               [(conj metrics spacing) new-end])))))

(defn detect-form-kind [form-loc]
  (let [children (node/children (z/node form-loc))]
    (assert (> (count children) 0))
    (or
      (let [first-child (first children)]
        (cond
          (node/linebreak? first-child) :empty-line
          (node/comment? first-child) :comment-block))
      (get-first-leaf-expr form-loc))))

(defn filter-valid [coll]
  (remove nil? coll))

(defn build-layout [form-loc]
  {:pre [(valid-loc? form-loc)
         (zip-utils/form? form-loc)]}
  (let [initial {:data {} :code [] :docs [] :headers [] :comments [] :line 0}
        {:keys [data code docs headers comments _line]} (build-form-layout initial (z/down form-loc))
        form-id (loc-id form-loc)
        form-kind (detect-form-kind form-loc)
        has-code? (not (empty? code))
        has-docs? (not (empty? docs))
        has-headers? (not (empty? headers))
        has-comments? (not (empty? comments))
        root-id (id/make form-id :root)
        code-id (id/make form-id :code)
        docs-id (id/make form-id :docs)
        comments-id (id/make form-id :comments)
        headers-id (id/make form-id :headers)]
    (cond-> data
      has-headers? (assoc headers-id {:tag      :headers
                                      :id       headers-id
                                      :children headers})
      has-comments? (assoc comments-id {:tag           :comments
                                        :id            comments-id
                                        :selectable?   true
                                        :section       :comments
                                        :spatial-index -1
                                        :children      comments
                                        :metrics       (compute-comments-metrics data comments)})
      has-docs? (assoc docs-id {:tag      :docs
                                :id       docs-id
                                :children docs})
      has-code? (assoc code-id {:tag      :code
                                :id       code-id
                                :children code})
      true (assoc root-id {:tag       :root
                           :id        root-id
                           :form-kind form-kind
                           :sections  {:headers  (if has-headers? headers-id)
                                       :docs     (if has-docs? docs-id)
                                       :code     (if has-code? code-id)
                                       :comments (if has-comments? comments-id)}}))))
