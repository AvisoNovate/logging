(ns user
  (:use
    clojure.repl
    io.aviso.exception
    speclj.config)
  (:require io.aviso.logging.setup))

(alter-var-root #'default-config assoc :color true :reporters ["documentation"])

(alter-var-root #'*default-frame-rules*
                conj [:name "speclj.running" :terminate])
