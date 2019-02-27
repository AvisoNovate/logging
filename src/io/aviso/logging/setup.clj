(ns io.aviso.logging.setup
  "Enables pretty exception reporting in the REPL and/or console, and sets up pretty exception logging
when using clojure.tools.logging. Also installs an default uncaught exception handler, and initializes
logging correlation id."
  (:require [io.aviso.logging.correlation]
            [io.aviso.repl :as repl]
            [io.aviso.logging :as logging]))

(repl/install-pretty-exceptions)
(logging/install-pretty-logging)
(logging/install-uncaught-exception-handler)
