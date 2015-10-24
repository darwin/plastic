(ns meld.tests.histograms
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.devcards.util :refer-macros [hist-card]]
            [meld.parser :as parser]))

; -------------------------------------------------------------------------------------------------------------------

(hist-card "symbol" #(parser/parse! "symbol"))
(hist-card "keyword" #(parser/parse! ":keword"))

(hist-card "list" #(parser/parse! "(1 2)") false)
(hist-card "nested" #(parser/parse! "[a b (1 2) 3]") true)
(hist-card "one" #(parser/parse! "  1  ") false)
(hist-card "comment" #(parser/parse! "  ;comment  ") false)
(hist-card "x-comment" #(parser/parse! "  x ;comment  ") false)

(hist-card "some-doc-example" #(parser/parse! "(ns n1.doc)

; independent comment
; block spanning
; 3 lines


; -----------------------
; under separator

(defn log-ex
  \"Middleware which catches and prints any handler-generated exceptions to console.
  Handlers are called from within a core.async go-loop, and core.async produces
  a special kind of hell when in comes to stacktraces. By the time an exception
  has passed through a go-loop its stack is mangled beyond repair and you'll
  have no idea where the exception was thrown.
  So this middleware catches and prints to stacktrace before the core.async sausage
  machine has done its work.
  \"
  [handler]
  (fn log-ex-handler
    [db v] ; looooooooooooong comment sdaadjasidiasodjaosd saijdoiad joadj asdj iosjdas ijd iosajdoasjd asj
    (warn \"re-frame: use of \"log-ex\" is deprecated.\")

    (try
      ; inlined comment
      (handler db v)
      (catch :default e                                     ;; ooops, handler threw
        (do
          (.error js/console (.-stack e))  ; side-comment
          ; spanning multiple lines
          (throw e)))))) ; more

; comment after
; in block

; another comment

(def test \"some string\") ; test comment
") false)
