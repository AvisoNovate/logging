(ns logging-spec
  (:use speclj.core
        io.aviso.logging.correlation)
  (:require io.aviso.logging.setup
            [clojure.tools.logging :as l])
  (:import [ch.qos.logback.classic.util ContextSelectorStaticBinder]
           [ch.qos.logback.core Appender]))


(defn- echo-handler
  [request]
  {:status  200
   :headers {}
   :body    {:cid *correlation-id*}})

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

(describe "MDC"

  (it "can incorporate the correlation-id into the MDC"
      (l/info "Forcing init of logging")
      (let [logged-text (atom nil)
            logger-root      (-> (ContextSelectorStaticBinder/getSingleton) .getContextSelector .getLoggerContext (.getLogger "ROOT"))
            ;; Haven't yet found a way to capture the formatted log output text, but this is pretty
            ;; safe: it's in the MDC ready to formatted.  Also a user can see it in the logs from the output
            capture-appender (reify Appender
                               (doAppend [_ event]
                                 (reset! logged-text
                                         (-> event .getMDCPropertyMap (.get "correlation-id")))))]
        (try
          (.addAppender logger-root capture-appender)
          (binding [*correlation-id* "XYZ"]
            (l/info "logging event") (should-contain "XYZ" @logged-text))
          (finally
            (.detachAppender logger-root capture-appender))))))

(run-specs)