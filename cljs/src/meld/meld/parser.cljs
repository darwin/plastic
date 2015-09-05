(ns meld.parser
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [cljs.tools.reader.reader-types :as rt]
            [cljs.tools.reader :as r]
            [meld.tracker :refer [source-tracker]]
            [meld.gray-matter :refer [process-gray-matter]]
            [meld.whitespace :refer [merge-whitespace]]
            [meld.ids :refer [assign-unique-ids!]]
            [meld.node :as node]))

(defn post-process-unit! [unit]
  (-> unit
    (process-gray-matter)
    (merge-whitespace)
    (assign-unique-ids!)))

(defn read-form [reader]
  (let [res (r/read {:eof ::eof-sentinel :read-cond :preserve} reader)]
    (if-not (keyword-identical? res ::eof-sentinel)
      res)))

; -------------------------------------------------------------------------------------------------------------------

(defn parse! [source name]
  (binding [rt/log-source* (partial source-tracker source)]
    (let [reader (rt/source-logging-push-back-reader source 1 name)
          read-next-form (partial read-form reader)]
      (dorun (take-while identity (repeatedly read-next-form)))
      (let [frames-atom (.-frames reader)
            top-level-nodes (:nodes @frames-atom)
            unit (node/make-unit top-level-nodes source name)
            post-processed-unit (post-process-unit! unit)]
        post-processed-unit))))

