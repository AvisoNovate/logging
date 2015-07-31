(ns io.aviso.logging.correlation
  "Defines a minimal namespace containing a dynamic Var used to propogate
  the correlation id into the logging layer."
  (:import [io.aviso.logging CorrelationIdAppender]
           [java.util UUID]))

(def ^:dynamic *correlation-id*
  "A dynamic var that contains the correlation id used by the `%mdc{correlation-id}` pattern.
  Defaults to the empty string."
  "")

;; This exposes the
(CorrelationIdAppender/setup #'*correlation-id*)

(defn default-correlation-id-generator
  "The default correlation id generator ignores the request, and returns a random UUID string.
  Alternate implementations may use information in the request to build a more meaningful or semantic
  name."
  [request]
  (str (UUID/randomUUID)))

(defn wrap-with-request-correlation
  "Wraps a handler with logic that obtains the correlation id from the request (if present).

  When the id is not present, a new id is generated.

  The id (from the request, or as generated) will be available in the [[*correlation-id*]] dynamic Var.
  This will allow code sending requests to other servers to add the necessary header.

  The id will be returned in the response."
  ([handler]
   (wrap-with-request-correlation handler "Correlation-Id" default-correlation-id-generator))
  ([handler header-name correlation-id-generator]
   (fn [request]
     (let [correlation-id (or (get-in request [:headers header-name])
                              (correlation-id-generator request))]
       (binding [*correlation-id* correlation-id]
         (when-let [response (handler request)]
           (assoc-in response [:headers header-name] correlation-id)))))))