package org.apache.mina.protocol;

import org.apache.mina.common.SessionManager;

/**
 * TODO document me.
 * <p>
 * {@link ProtocolHandlerFilter}s can be added and removed at any time to filter
 * events just like Servlet filters and they are effective immediately.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 *
 */
public interface ProtocolSessionManager extends SessionManager {
    ProtocolHandlerFilterChain getFilterChain();
}
