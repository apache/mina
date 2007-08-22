package org.apache.mina.filter.logging;

import org.apache.mina.common.*;
import org.apache.mina.filter.util.WrappingFilter;
import org.slf4j.MDC;

import java.net.InetSocketAddress;
import java.util.*;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 566952 $, $Date: 2007-08-17 09:25:04 +0200 (vr, 17 aug 2007) $
 */

public class MdcLoggingFilter extends WrappingFilter {

    /** key used for storing the context map in the IoSession */
    public static final String CONTEXT_KEY = MdcLoggingFilter.class + ".CONTEXT_KEY";

    protected void wrapFilterAction(IoEventType eventType, IoSession session, WrappingFilter.FilterAction action) {
        //noinspection unchecked
        Map<String,String> context = getContext(session);
        /* copy context to the MDC */
        for (Map.Entry<String, String> e : context.entrySet()) {
            MDC.put(e.getKey(), e.getValue());
        }
        try {
            /* propagate event down the filter chain */
            action.execute();
        } finally {
            /* remove context from the MDC */
            for (String key : context.keySet()) {
                MDC.remove(key);
            }
        }
    }    

    public Map<String,String> getContext(final IoSession session) {
        //noinspection unchecked
        Map<String,String> context = (Map<String,String>) session.getAttribute(CONTEXT_KEY);
        if (context == null) {
            context = new HashMap<String, String>();
            fillContext(session, context);
            session.setAttribute(CONTEXT_KEY, context);
        }
        return context;
    }

    /**
     * write key properties of the session to the Mapped Diagnostic Context
     * sub-classes could override this method to map more/other attributes
     * @param session the session to map
     * @param context key properties will be added to this map
     */
    protected void fillContext(final IoSession session, final Map<String,String> context) {
        context.put("IoHandlerClass", session.getHandler().getClass().toString());
        context.put("remoteAddress", session.getRemoteAddress().toString());
        context.put("localAddress", session.getLocalAddress().toString());
        if (session.getTransportMetadata().getAddressType() == InetSocketAddress.class) {
            InetSocketAddress remoteAddress = (InetSocketAddress) session.getRemoteAddress();
            InetSocketAddress localAddress  = (InetSocketAddress) session.getLocalAddress();
            context.put("remoteIp", remoteAddress.getAddress().getHostAddress());
            context.put("remotePort", String.valueOf(remoteAddress.getPort()));
            context.put("localIp", localAddress.getAddress().getHostAddress());
            context.put("localPort", String.valueOf(localAddress.getPort()));
        }
    }

}
