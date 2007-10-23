package org.apache.mina.filter.keepalive;

import org.apache.mina.handler.chain.IoHandlerCommand;

/**
 * Abstract superclass for KeepAliveFilters.
 * Implements a few common variables and constructors.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractKeepAliveFilter implements IoHandlerCommand {
    /**
     * Default message for pinging.
     */
    public static final String PING = "PING";

    /**
     * Default message for replying to pings.
     */
    public static final String PONG = "PONG";

    /**
     * Object identifying the ping messages for this KeepAliveFilter.
     */
    protected final Object pingMessage;

    /**
     * Object identifying the pong messages for this KeepAliveFilter.
     */
    protected final Object pongMessage;

    /**
     * Sets the <code>pingMessage</code> to <code>PING</code> and the
     * <code>pongMessage</code> to <code>PONG</code>.
     */
    public AbstractKeepAliveFilter() {
        this.pingMessage = PING;
        this.pongMessage = PONG;
    }

    /**
     * Sets <code>pingMessage</code> and <code>pongMessage</code> to the
     * specified parameters.
     * @param pingMessage
     * @param pongMessage
     */
    public AbstractKeepAliveFilter(Object pingMessage, Object pongMessage) {
        assert pingMessage != null;
        assert pongMessage != null;

        this.pingMessage = pingMessage;
        this.pongMessage = pongMessage;
    }
}