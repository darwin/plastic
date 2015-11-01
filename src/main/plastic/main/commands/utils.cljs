(ns plastic.main.commands.utils
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]))

; -------------------------------------------------------------------------------------------------------------------

(defn toggle-setting [db key]
  (update-in db [:settings key] not))