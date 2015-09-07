(ns meld.parser
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [cljs.tools.reader.reader-types :as rt]
            [cljs.tools.reader :as r]
            [meld.tracker :refer [make-tracker]]
            [meld.gray-matter :refer [process-gray-matter]]
            [meld.whitespace :refer [merge-whitespace]]
            [meld.node :as node]))

(defn find-top-level-nodes-ids [meld]
  (map first (remove (fn [[id _node]] (:parent (get meld id))) meld)))

(defn define-unit [meld source name]
  (let [top-level-ids (find-top-level-nodes-ids meld)
        unit (node/make-unit top-level-ids source name)
        unit-id (:id unit)
        meld* (transient meld)
        meld*! (volatile! meld*)]
    (vswap! meld*! assoc! unit-id unit)
    (vswap! meld*! assoc! :top unit-id)
    (doseq [id top-level-ids]
      (vswap! meld*! assoc! id (assoc (get meld* id) :parent (:id unit))))
    (persistent! @meld*!)))

(defn post-process-meld! [meld source name]
  (-> meld
    (define-unit source name)
    (process-gray-matter source)                                                                                      ; gray matter is whitespace, linebreaks and comments, we deal with it in this second pass
    (merge-whitespace)))                                                                                              ; want to merge whitespace nodes into following non-whitespace nodes

(defn read-form! [reader]
  (let [opts {:eof       :eof-sentinel
              :read-cond :preserve}
        res (r/read opts reader)]
    (if-not (keyword-identical? res :eof-sentinel)
      res)))

; -------------------------------------------------------------------------------------------------------------------

(defn parse! [source name]
  (let [[tracker! flush!] (make-tracker source)]
    (binding [rt/log-source* tracker!]
      (let [reader (rt/source-logging-push-back-reader source 1 name)
            read-next-form! (partial read-form! reader)]
        (dorun (take-while identity (repeatedly read-next-form!)))                                                    ; read all avail forms
        (post-process-meld! (flush!) source name)))))
