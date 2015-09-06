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
    (process-gray-matter)                                                                                             ; gray matter is whitespace, linebreaks and comments, we deal with it in this second pass
    (merge-whitespace)                                                                                                ; want to merge whitespace nodes into following non-whitespace nodes
    (assign-unique-ids!)))                                                                                            ; each node gets an unique id for easy addressing

(defn read-form [reader]
  (let [opts {:eof       ::eof-sentinel
              :read-cond :preserve}
        res (r/read opts reader)]
    (if-not (keyword-identical? res ::eof-sentinel)
      res)))

; -------------------------------------------------------------------------------------------------------------------

(defn parse! [source name]
  (binding [rt/log-source* (partial source-tracker source)]                                                           ; piggyback on log-source* to extract metadata for our second pass via source-tracker
    (let [reader (rt/source-logging-push-back-reader source 1 name)
          read-next-form (partial read-form reader)]
      (dorun (take-while identity (repeatedly read-next-form)))                                                       ; read all avail forms
      (let [top-level-nodes (:nodes @(.-frames reader))                                                               ; extract our collected data from reader
            unit (node/make-unit top-level-nodes source name)]                                                        ; unit is a special node representing whole file
        (post-process-unit! unit)))))
