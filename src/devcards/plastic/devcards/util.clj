(ns plastic.devcards.util
  (:require [devcards.util.utils]))

(defmacro deftest [vname & parts]
  `(devcards.core/deftest ~vname ~@parts))

(defmacro meld-zip-card [vname & args]
  `(devcards.core/defcard* ~vname
    (plastic.devcards.util/zip-card* ~vname ~@args)))

(defmacro meld-card [vname & args]
  `(devcards.core/defcard* ~vname
     (plastic.devcards.util/meld-card* ~vname ~@args)))

(defmacro hist-card [vname & args]
  `(devcards.core/defcard* ~vname
     (plastic.devcards.util/hist-card* ~vname ~@args)))