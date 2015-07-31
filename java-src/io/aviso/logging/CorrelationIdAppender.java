package io.aviso.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import clojure.lang.Var;

/**
 * Responsible for updating the ILoggingEvent with key "correlation-id" based on the
 * dynamic Var. This appender doesn't really append, and must be ordered first in the
 * {@code <root>} element of the logback configuration file.
 */
public class CorrelationIdAppender extends AppenderBase<ILoggingEvent> {

    /**
     * This Var is provided from the io.aviso.logging.correlation namespace.
     */
    private static Var correlationId;

    public static void setup(Var var) {
        correlationId = var;
    }

    public static String getCurrentCorrelationId() {
        return correlationId.get().toString();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        eventObject.getMDCPropertyMap().put("correlation-id", getCurrentCorrelationId());
    }
}
