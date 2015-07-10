(ns quark.cogs.editor.layout.soup
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn emit-token [token info]
  (assoc token :geometry info))

(defn build-soup-render-info [tokens geometry]
  (map (fn [[id info]] (emit-token (get tokens id) info)) geometry))