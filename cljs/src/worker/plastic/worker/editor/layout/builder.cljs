(ns plastic.worker.editor.layout.builder
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [clojure.zip :as z]
            [plastic.worker.editor.layout.utils :as layout-utils]
            [plastic.util.zip :as zip-utils :refer [valid-loc?]]
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
    (layout-utils/string-node? node) (layout-utils/prepare-string-for-display (node/string node))
    (layout-utils/keyword-node? node) (helpers/strip-colon (node/string node))
    :else (node/string node)))

(defn is-newline? [loc]
  (or (nil? loc) (node/linebreak? (z/node loc))))

(defn break-locs-into-lines [accum loc]
  (let [new-accum (assoc accum (dec (count accum)) (conj (last accum) loc))]
    (if (is-newline? loc)
      (conj new-accum [])
      new-accum)))

(defn is-simple? [loc]
  (if (nil? loc)
    true
    (not (node/inner? (z/node loc)))))

(defn is-double-column-line? [line]
  (and (is-simple? (first line)) (not (is-newline? (second line))) (is-newline? (nth line 2 nil))))

(defn prepend-spot [table node-id]
  (let [spot-id (id/make-spot node-id)
        [opts & lines] table
        [hints & line] (first lines)]
    (cons opts (cons (cons hints (cons spot-id line)) (rest lines)))))

(defn nl-near-doc? [loc]
  (or
    (utils/is-nl-near-doc? loc zip-left)
    (utils/is-nl-near-doc? loc zip-right)))

(defn prepare-children-table [locs]
  (let [locs-without-docs (remove utils/is-doc? (remove nl-near-doc? locs))
        lines (reduce break-locs-into-lines [[]] locs-without-docs)]
    (if (<= (count lines) 1)
      [{:oneliner? true} (cons {} (map zip-utils/loc-id (first lines)))]
      (let [first-line-is-double-column? (is-double-column-line? (first lines))]
        (cons {:multiline? true}
          (for [line lines]
            (if (is-double-column-line? line)
              (let [indent? (and (not first-line-is-double-column?) (not= line (first lines)))]
                (cons {:double-column? true :indent indent?} (map zip-utils/loc-id line)))
              (let [indent? (not= line (first lines))]
                (cons {:indent indent?} (map zip-utils/loc-id line))))))))))

(defn ignored-newline? [loc]
  (nl-near-doc? loc))

(defn process-children [loc]
  (-> loc
    (child-locs)
    (prepare-children-table)
    (prepend-spot (zip-utils/loc-id loc))))

(defn add-code-item [accum loc]
  (let [node (zip/node loc)
        node-id (:id node)
        inner? (node/inner? node)
        prev-loc (zip-prev loc)
        after-nl? (and (is-newline? prev-loc) (not (ignored-newline? prev-loc)))]
    (assoc-in accum [:data node-id]
      (cond-> {:id   node-id
               :line (:line accum)
               :tag  (node/tag node)}
        (is-newline? loc) (assoc :tag :newline)
        inner? (assoc :children (process-children loc))
        after-nl? (assoc :after-nl true)
        (not inner?) (assoc :text (prepare-node-text node))
        (layout-utils/is-selectable? (node/tag node)) (assoc :selectable? true)
        (layout-utils/string-node? node) (assoc :type :string)
        (layout-utils/keyword-node? node) (assoc :type :keyword)))))

(defn add-doc-item [accum loc]
  (let [node (z/node loc)
        node-id (:id node)
        text (node/string node)]
    (assoc-in accum [:data node-id]
      {:id          node-id
       :tag         :token
       :type        :doc
       :selectable? true
       :line        -1
       :text        (layout-utils/prepare-string-for-display text)})))

(defn add-spot-item [accum loc]
  (let [node (zip/node loc)
        node-id (:id node)
        spot-id (id/make-spot node-id)]
    (assoc-in accum [:data spot-id] {:id          spot-id
                                     :tag         :token
                                     :type        :spot
                                     :line        (:line accum)
                                     :selectable? true
                                     :text        ""})))

(defn lookup-def-arities [accum loc]
  (let [node (zip/node loc)
        node-id (:id node)]
    (if-let [arities (utils/lookup-arities loc)]
      (assoc-in accum [:data node-id :arities] arities)
      accum)))

(defn build-node-layout [accum loc]
  (let [node (zip/node loc)
        node-id (:id node)
        layout-item (fn [accum] (cond
                                  (nl-near-doc? loc) accum
                                  (utils/is-doc? loc) (-> accum
                                                        (add-doc-item loc)
                                                        (update :docs #(conj % node-id)))
                                  (utils/is-def-name? loc) (-> accum
                                                             (add-code-item loc)
                                                             (lookup-def-arities loc)
                                                             (update :headers #(conj % node-id)))
                                  :else (cond-> accum
                                          (node/inner? node) (add-spot-item loc)
                                          true (add-code-item loc))))
        detect-new-lines (fn [accum] (if (and (is-newline? loc) (not (ignored-newline? loc)))
                                       (update accum :line inc) accum))]
    (-> accum
      layout-item
      detect-new-lines)))

(defn get-first-leaf-expr [loc]
  (let [bottom-loc (last (take-while valid-loc? (iterate zip-down loc)))]
    (if (valid-loc? bottom-loc)
      (node/string (z/node bottom-loc)))))

(defn build-layout [form-loc]
  (let [form-id (zip-utils/loc-id form-loc)
        root-id (id/make form-id :root)
        code-id (id/make form-id :code)
        docs-id (id/make form-id :docs)
        headers-id (id/make form-id :headers)
        locs (take-while valid-loc? (iterate zip-next form-loc))
        initial {:data {} :docs [] :headers [] :line 0}
        {:keys [data docs headers _line]} (reduce build-node-layout initial locs)]
    (-> data
      (assoc root-id {:tag      :tree
                      :id       root-id
                      :children [headers-id docs-id code-id]})
      (assoc code-id {:tag       :code
                      :id        code-id
                      :form-kind (get-first-leaf-expr form-loc)
                      :children  [(zip-utils/loc-id form-loc)]})
      (assoc docs-id {:tag      :docs
                      :id       docs-id
                      :children docs})
      (assoc headers-id {:tag      :headers
                         :id       headers-id
                         :children headers}))))
