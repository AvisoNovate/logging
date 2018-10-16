(ns logging-spec
  (:use speclj.core
        io.aviso.logging.correlation
        io.aviso.logging.mdc
        io.aviso.logging.capture)
  (:require
    [clojure.tools.logging :as l])
  (:import [ch.qos.logback.classic.util ContextSelectorStaticBinder]
           [ch.qos.logback.core Appender]))

(defmacro capturing
  "Adapt the fixture (for clojure.test) to this spec test."
  [& body]
  `(logged-events-fixture (fn [] ~@body)))

(defn stripped-logged-events
  []
  (map #(dissoc % :timestamp :thread-name) (logged-events)))

(defn- echo-handler
  [_request]
  {:status  200
   :headers {}
   :body    {:cid *correlation-id*}})

(l/info "Forcing logging initialization ....")

(describe "request correlation"

  (with-all std-pipeline (wrap-with-request-correlation echo-handler))

  (it "generates an id if missing"
      (let [response (@std-pipeline {})
            cid      (-> response :body :cid)
            header   (get-in response [:headers "Correlation-Id"])]
        (should-not-be-nil cid)
        (should-be-same cid header)))

  (it "uses the provided id"
      (let [response (@std-pipeline {:headers {"Correlation-Id" "preset"}})]
        (should= {:status  200
                  :headers {"Correlation-Id" "preset"}
                  :body    {:cid "preset"}}
                 response)))

  (it "can override defaults"
      (let [pipeline (wrap-with-request-correlation echo-handler "Custom-Header" (constantly "override"))
            response (pipeline {})]
        (should= {:status  200
                  :headers {"Custom-Header" "override"}
                  :body    {:cid "override"}}
                 response))))



(defmacro capturing-mdc
  [sym & body]
  `(do
     (let [~sym (atom nil)
           logger-root# (-> (ContextSelectorStaticBinder/getSingleton)
                            .getContextSelector
                            .getLoggerContext
                            (.getLogger "ROOT"))
           ;; Haven't yet found a way to capture the formatted log output text, but this is pretty
           ;; safe: it's in the MDC ready to formatted.  Also a user can see it in the logs from the output
           capture-appender# (reify Appender
                               (doAppend [_ event#]
                                 (reset! ~sym
                                         (into {} (.getMDCPropertyMap event#)))))]
       (try
         (.addAppender logger-root# capture-appender#)
         ~@body
         (finally
           (.detachAppender logger-root# capture-appender#))))))

(describe "correlation MDC"

  (it "can incorporate the correlation-id into the MDC"
      (capturing-mdc mdc
        (binding [*correlation-id* "XYZ"]
          (l/info "logging event")
          ;; Due to leakiness in the MDC, a re-run of this test may
          ;; still include "magic" => "ring".
          (should= "XYZ" (get @mdc "correlation-id"))))))

(describe "extra MDC"
  (it "can incorporate extra MDC"
      (capturing-mdc mdc
        (with-mdc {:magic "ring"}
                  (l/info "extra mdc logging event")
                  (should= "ring" (get @mdc "magic")))))

  (it "ignores nil values"
      (capturing-mdc mdc
        (with-mdc {:nothing nil}
                  (l/info "nothing logging")
                  (should= false
                           (contains? @mdc "nothing")))))

  (it "prevents leakage when defaults are available"
      (set-mdc-default {:leaky "default"})
      (capturing-mdc mdc
        (l/info "checking default")
        (should= "default"
                 (get @mdc "leaky")))

      (capturing-mdc mdc
        (with-mdc {:leaky "override"}
                  (l/info "inside with-mdc")
                  (should= {"leaky" "override"}
                           *extra-mdc*)
                  (should= "override"
                           (get @mdc "leaky"))))

      (capturing-mdc mdc
        (l/info "checking default restored")
        (should= "default"
                 (get @mdc "leaky")))))

(describe "capture"
  ;; Meant to be used with EDN formatted log data, e.g., io.pedestal/pedestal-log
  (it "captures logged events"
      (capturing
        (l/info "{:working :great}")
        (should= [{:logger :logging-spec
                   :level :info
                   :data {:working :great}}]
                 (stripped-logged-events)))
      (let [logged (first (logged-events))]
        (should-not-be-nil (:timestamp logged))
        (should= (-> (Thread/currentThread) .getName)
                 (:thread-name logged))))
  (it "handles unparsable EDN"
      ;; In practice this is usually due to a #object or other
      ;; non-value.
      (capturing (l/info "{ not valid")
                 (should= {:message "{ not valid"}
                          (-> (logged-events) first :data))))

  (it "exposes exceptions"
      (capturing
        (try
          (throw (IllegalStateException. "How did this happen?"))
          (catch Throwable t
            (l/error (ex-info "Unexpected failure." {} t)
                     "{:event :failure}")))
        (should= [{:logger :logging-spec
                   :level :error
                   :data {:event :failure}
                   :ex {:message "Unexpected failure."
                        :class-name "clojure.lang.ExceptionInfo"
                        :cause {:message "How did this happen?"
                                :class-name "java.lang.IllegalStateException"}}}]
                 (stripped-logged-events)))))

(run-specs)
