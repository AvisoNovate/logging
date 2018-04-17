(ns io.aviso.logging.capture
  "Capture logged events for in-process debugging and testing."
  {:added "0.3.0"}
  (:require
    [clojure.edn :as edn]
    [clojure.string :as str])
  (:import
    (ch.qos.logback.classic Logger Level)
    (org.slf4j LoggerFactory)
    (ch.qos.logback.core.read ListAppender)
    (ch.qos.logback.classic.spi LoggingEvent IThrowableProxy)))

;; Because Appender.append() is protected, we can't simply conj into a vector the way I'd prefer.
;; Not without writing some Java code.
;; Instead, we'll use this built-in class.

(def ^:private ^ListAppender log-appender
  (doto
    (ListAppender.)
    .start
    (.setName "CAPTURE")))

(defn clear-logged-events
  "Clears any captured log events."
  []
  (-> log-appender .list .clear))

(defn ^:private throwable-proxy->map
  [^IThrowableProxy proxy]
  (let [nested (.getCause proxy)
        recurse? (and (some? nested)
                      (not= proxy nested))]
    (cond-> {:message (.getMessage proxy)
             :class-name (.getClassName proxy)}
      recurse? (assoc :cause (throwable-proxy->map nested)))))

(defn logged-events
  "Returns a snapshot, as a vector, of all events logged since the last clear of the log (the start
  of the current test).

  Event maps have the following keys:

  :level
  : :trace, :debug, etc.

  :logger
  : The name of the logger, as a keyword

  :data
  : A map of the data parsed as EDN from the event message. If the EDN data can't be parsed,
    then this will be a map of `:message` and the message string.

  :timestamp
  : System time (milliseconds since epoch) of the event.

  :thread-name
  : Name of thread on which the event was logged.

  :ex
  : The exception associated with the event. This is a map with keys :message and :class-name,
    and optionally :cause.

  Returns a lazy seq of events. However, note that the seq is based on a mutable list
  that will be unstable if further logging takes place."
  []
  (let [level->keyword (memoize
                         (fn [^Level level]
                           (-> level str str/lower-case keyword)))
        message->data (fn [message]
                        (try
                          (edn/read-string message)
                          (catch Throwable _
                            {:message message})))
        event->map (fn [^LoggingEvent e]
                     (let [ex (.getThrowableProxy e)]
                       (prn `capture ex)
                       (cond-> {:logger (-> e .getLoggerName keyword)
                                :level (-> e .getLevel level->keyword)
                                :timestamp (.getTimeStamp e)
                                :thread-name (.getThreadName e)
                                :data (-> e .getMessage message->data)}
                         ex (assoc :ex (throwable-proxy->map ex)))))]
    (->> log-appender
         .list
         ;; mapv would be safer, but I believe in the context of when we'll be running this code
         ;; this will be good enough
         (map event->map))))


(defn logged-events-fixture
  "Using the expected logback configuration, installs an appender that captures logged EDN data into
  the *log-data atom.

  The appender is removed at the end of the test BUT the captured log data is kept until the start
  of the next test.

  NOTE: When using `io.aviso.logging/install-pretty-logging`, the
  `:ex` data will always be nil (`install-pretty-logging` overrides
  the message with a pretty-formatted exception, which results in
  the exception data seen by the appender being nil."
  [f]
  (clear-logged-events)
  (let [root ^Logger (LoggerFactory/getLogger "ROOT")]
    (try
      (.addAppender root log-appender)
      (f)
      (finally
        (.detachAppender root log-appender)))))
