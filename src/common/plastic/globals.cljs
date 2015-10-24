(ns plastic.globals
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]))

; -------------------------------------------------------------------------------------------------------------------
; think twice before you add something here
; ideal situation would be to not to rely on any global state at all ...
; ... but we don't live in an ideal world

; see https://github.com/stuartsierra/component for discussion

; ===================================================================================================================
;
;                                          ☠ STOP! AND THINK AGAIN! ☠
;
; ===================================================================================================================

; hold a visible reference just for debugging purposes, never access this directly
(defonce ^:dynamic *system* nil)

; this is just for debug-printing callstack situation in fancy logs
(defonce ^:dynamic *current-thread* nil)

; for debugging purposes when db validator throws
(defonce ^:dynamic *current-event-stack* [])

; indentation-level for debug-printing how zip operations nest
(defonce ^:dynamic *zip-op-nesting* 0)
