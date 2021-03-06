(ns meld.parser
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [cljs.tools.reader.reader-types :as rt]
            [cljs.tools.reader :as r]
            [meld.tracker :refer [make-tracker eof-sentinel]]
            [meld.gray-matter :refer [process-gray-matter]]
            [meld.whitespace :refer [merge-whitespace]]
            [meld.file :refer [wrap-all-as-file]]
            [meld.unit :refer [group-into-units]]))

; -------------------------------------------------------------------------------------------------------------------

(defn post-process-meld! [meld source name]
  (-> meld
    (wrap-all-as-file source name)
    (process-gray-matter source)                                                                                      ; gray matter is whitespace, linebreaks and comments, we deal with it in this second pass
    (group-into-units)
    (merge-whitespace)))                                                                                              ; want to merge whitespace nodes into following non-whitespace nodes or parents

(defn read-form! [reader]
  (let [opts {:eof       eof-sentinel
              :read-cond :preserve}
        res (r/read opts reader)]
    (if-not (keyword-identical? res eof-sentinel)
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
