(ns quark.cogs.editor.render.soup
  (:require [quark.cogs.editor.render.utils :refer [raw-html wrap-specials id! classv]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn soup-component [form]
  [:div.soup
   (for [item (:soup form)]
     (if (= (:tag item) :newline)
       ^{:key (id!)} [:br]
       ^{:key (id!)} [:div.soup-item
                      (:string item)]))])