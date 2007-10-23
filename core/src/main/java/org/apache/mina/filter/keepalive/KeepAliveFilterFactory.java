package org.apache.mina.filter.keepalive;

import java.util.concurrent.ScheduledExecutorService;

import org.apache.mina.common.IoSession;

/**
 * A factory that creates protocol level filters, that implement
 * a simple keepalive routine for the connection. The factory has
 * "settings" so it is not safe to be used by concurrent threads.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see org.apache.mina.handler.chain.ChainedIoHandler
 */
public class KeepAliveFilterFactory {
    private ScheduledExecutorService scheduler;

    private Object pingMessage;

    private Object pongMessage;

    private long pingingInterval;

    private long waitingTime;

    /**
     * PassiveKeepAliveFilters may be shared, so the factory keeps
     * a cache. NOTE: the usage pattern favors a "lazy" initialization.
     */
    private PassiveKeepAliveFilter cache;

    /**
     * Creates a new KeepAliveFilterFactory with the specified
     * <code>scheduler</code>, default ping&pong messages and the
     * default <code>pingingInterval</code> 15000 milliseconds and
     * default <code>waitingTime</code> 35000 milliseconds.
     * @param scheduler
     */
    public KeepAliveFilterFactory(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        this.pingMessage = AbstractKeepAliveFilter.PING;
        this.pongMessage = AbstractKeepAliveFilter.PONG;
        this.pingingInterval = 15000;
        this.waitingTime = 35000;
    }

    public void setScheduler(ScheduledExecutorService scheduler) {
        if (scheduler != null)
            this.scheduler = scheduler;
    }

    public void setPingMessage(Object pingMessage) {
        if (this.pingMessage != pingMessage)
            cache = null;
        this.pingMessage = pingMessage;
    }

    public void setPongMessage(Object pongMessage) {
        if (this.pongMessage != pongMessage)
            cache = null;
        this.pongMessage = pongMessage;
    }

    public void setPingingInterval(long pingingInterval) {
        this.pingingInterval = pingingInterval;
    }

    public void setWaitingTime(long waitingTime) {
        this.waitingTime = waitingTime;
    }

    /**
     * Creates a new ActiveKeepAliveFilter for the IoSession
     * <code>session</code>.
     * @param session Iosession of the connection.
     * @return new ActiveKeepAliveFilter for <code>session</code>. 
     */
    public ActiveKeepAliveFilter createActiveKeepAliveFilter(IoSession session) {
        return new ActiveKeepAliveFilter(session, scheduler, pingingInterval,
                waitingTime);
    }

    /**
     * @return a PassiveKeepAliveFilter.
     */
    public PassiveKeepAliveFilter createPassiveKeepAliveFilter() {
        if (cache == null)
            cache = new PassiveKeepAliveFilter(pingMessage, pongMessage);
        return cache;
    }
}