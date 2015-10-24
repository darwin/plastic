(ns meld.helpers
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [cljs.tools.reader.reader-types :refer [read-char unread]]
            [meld.node :as node])
  (:import goog.string.StringBuffer))

; -------------------------------------------------------------------------------------------------------------------

(def ws-rx #"[\s]")

(defn ^boolean linebreak? [c]
  (or (identical? \newline c) (identical? "\n" c) (nil? c)))

(defn ^boolean whitespace? [ch]
  (when-not (or (nil? ch) (linebreak? ch))
    (if (identical? ch \,)
      true
      (.test ws-rx ch))))

; -------------------------------------------------------------------------------------------------------------------

(defn read-line [reader init]
  (loop [buf (StringBuffer. init)
         char (read-char reader)]
    (if (linebreak? char)
      (do
        (unread reader char)
        (str buf))
      (recur (.append buf char) (read-char reader)))))

(defn read-whitespace [reader init]
  (loop [buf (StringBuffer. init)
         char (read-char reader)]
    (if-not (whitespace? char)
      (do
        (unread reader char)
        (str buf))
      (recur (.append buf char) (read-char reader)))))

; -------------------------------------------------------------------------------------------------------------------

(defn slurp-comment [reader init]
  (node/make-node :comment (read-line reader init)))

(defn slurp-whitespace [reader init]
  (node/make-node :whitespace (read-whitespace reader init)))

(defn slurp-linebreak [_reader]
  (node/make-linebreak))

; -------------------------------------------------------------------------------------------------------------------

(defn skip-chunk [reader size]
  (dorun (repeatedly size #(read-char reader)))
  size)
