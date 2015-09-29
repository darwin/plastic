(ns plastic.devcards.util
  (:require [devcards.util.utils :as utils]))

(defmacro deftest [vname & parts]
  `(devcards.core/deftest ~vname ~@parts))

(defmacro def-zip-card [& args]
  (if (utils/devcards-active?)
    `(plastic.devcards.util/def-zip-card* ~@args)))

(defmacro def-source-zip-card [& args]
  (if (utils/devcards-active?)
    `(plastic.devcards.util/def-source-zip-card* ~@args)))

(defmacro def-meld-card [& args]
  (if (utils/devcards-active?)
    `(plastic.devcards.util/def-meld-card* ~@args)))

(defmacro def-hist-card [& args]
  (if (utils/devcards-active?)
    `(plastic.devcards.util/def-hist-card* ~@args)))

(defmacro def-meld-data-card [& args]
  (if (utils/devcards-active?)
    `(plastic.devcards.util/def-meld-data-card* ~@args)))