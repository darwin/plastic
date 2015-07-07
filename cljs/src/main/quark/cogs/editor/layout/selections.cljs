(ns quark.cogs.editor.layout.selections
  (:require [quark.cogs.editor.utils :refer []])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn emit-item [selectable info]
  (assoc selectable :geometry info))

(defn build-selections-render-info [selectables geometry]
  (map (fn [[id info]] (emit-item (get selectables id) info)) geometry))