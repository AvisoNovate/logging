(ns io.aviso.logging.mdc
  "Allows arbitrary keys and values to be added to the event MDC."
  {:added "0.2.0"}
  (:import (io.aviso.logging ExtraMDCAppender)))

(def ^:dynamic *extra-mdc*
  "May contain a map of extra values to add to the logging MDC (message diagnostic context).

  The keys of the map are converted to string (via clojure.core/name).  The values of the map
  are converted to string via .toString(); null values are not added to the MDC.

  Note: the appender adds values to the MDC, which is stored as a mutable value by Logback.
  Values stored into the MDC may \"stick\" in the per-thread MDC data and be visible in
  later logging, even after exiting the [[with-mdc]] block.

  For best results, invoke [[set-mdc-default]] at application startup, setting
  a default for each key that may later be added to the MDC
  by a call to [[with-mdc]]."
  nil)

(ExtraMDCAppender/setup #'*extra-mdc*)

(defn set-mdc-default
  "Alters the root value for the [[*extra-mdc*]] var, setting a key and value.
  The value is usually an empty string.
  This ensures that MDC set by [[with-mdc]] doesn't leak out into subsequent
  logging on the same thread."
  [key value]
  (alter-var-root #'*extra-mdc* assoc key value))

(defmacro with-mdc
  "Binds a key/value pair into [[*extra-mdc*]] before evaluating the body."
  [key value & body]
  `(binding [*extra-mdc* (assoc *extra-mdc* ~key ~value)]
     ~@body))