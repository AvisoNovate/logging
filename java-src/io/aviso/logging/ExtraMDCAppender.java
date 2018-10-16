package io.aviso.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Var;
import org.slf4j.MDC;

import java.util.Map;

/**
 * Responsible for adding any number of keys to the event's MDC (this appender
 * does not actually do any appending).  A dynamic Clojure var,
 * io.aviso.logging.mdc/*extra-mdc*, is the source.
 */
public class ExtraMDCAppender extends AppenderBase<ILoggingEvent> {
    /**
     * This Var is provided from the io.aviso.logging.mdc namespace.
     */
    private static Var mdcVar;

    private final IFn nameFn = Clojure.var("clojure.core", "name");

    public static void setup(Var var) {
        mdcVar = var;
    }

    public static Map<?, ?> getMDCVarMap() {
        return (Map<?, ?>) mdcVar.get();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        Map<?, ?> varMap = getMDCVarMap();

        if (varMap != null && !varMap.isEmpty()) {

            for (Map.Entry<?, ?> entry : varMap.entrySet()) {
                Object value = entry.getValue();

                if (value != null) {
                    // Note: once these values go into the MDC, they can be "sticky"
                    // even after the real Appender does its work, and after
                    // the Clojure code exits the logging code and even the
                    // with-mdc block.  See the notes in the namespace.
                    MDC.put((String) nameFn.invoke(entry.getKey()),
                            value.toString());
                }
            }
        }
    }
}
