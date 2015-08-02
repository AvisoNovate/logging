(ns user
  (:use
    clojure.repl
    io.aviso.exception
    speclj.config))

(alter-var-root #'default-config assoc :color true :reporters ["documentation"])

(alter-var-root #'*default-frame-rules*
                conj [:name "speclj.running" :terminate])
