(ns plastic.main.commands.utils
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.schema.paths :as paths]))

(defn toggle-setting [db key]
  (update-in db (conj paths/settings key) not))