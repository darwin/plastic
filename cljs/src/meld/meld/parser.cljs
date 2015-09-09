(ns meld.parser
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [cljs.tools.reader.reader-types :as rt]
            [cljs.tools.reader :as r]
            [meld.tracker :refer [make-tracker]]
            [meld.gray-matter :refer [process-gray-matter]]
            [meld.whitespace :refer [merge-whitespace]]
            [meld.meld :refer [define-unit]]))

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

(defn parse!
  ([source] (parse! source nil))
  ([source name]
   (let [[tracker! flush!] (make-tracker source)]
     (binding [rt/log-source* tracker!]
       (let [reader (rt/source-logging-push-back-reader source 1 name)
             read-next-form! (partial read-form! reader)]
         (dorun (take-while identity (repeatedly read-next-form!)))                                                   ; read all avail forms
         (post-process-meld! (flush!) source name))))))
