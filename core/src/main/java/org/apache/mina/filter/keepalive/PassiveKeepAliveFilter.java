package org.apache.mina.filter.keepalive;

import org.apache.log4j.Logger;
import org.apache.mina.common.IoSession;

/**
 * A protocol level filter which filters keepalive messages and sends
 * keepalive responses. This class is stateless and may be shared by
 * several connections. 
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class PassiveKeepAliveFilter extends AbstractKeepAliveFilter {
    private final Logger logger = Logger.getLogger(this.getClass());

    /**
     * @see AbstractKeepAliveFilter#AbstractKeepAliveFilter()
     */
    public PassiveKeepAliveFilter() {
        super();
    }

    /**
     * @see AbstractKeepAliveFilter#AbstractKeepAliveFilter(Object, Object)
     * @param pingMessage
     * @param pongMessage
     */
    public PassiveKeepAliveFilter(Object pingMessage, Object pongMessage) {
        super(pingMessage, pongMessage);
    }

    /**
     * If <code>pingMessage</code> equals <code>message</code>  ,
     * reply with <code>pongMessage</code>. Otherwise forward to
     * <code>next</code>.  
     */
    public void execute(NextCommand next, IoSession session, Object message)
            throws Exception {
        if (pingMessage.equals(message)) {
            if (logger.isDebugEnabled())
                logger.debug("Replying PONG");
            session.write(pongMessage);
        } else
            next.execute(session, message);
    }
}