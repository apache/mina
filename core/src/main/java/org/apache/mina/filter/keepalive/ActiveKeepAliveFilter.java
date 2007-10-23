package org.apache.mina.filter.keepalive;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.mina.common.IoSession;

/**
 * A protocol level filter which will send keepalive messages
 * and filter keepalive responses. If no responses are received
 * in a specified timeframe, the filter will close the IoSession.
 * This filter has state information so it should not be shared by
 * multiple connections.  
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * 
 * FIXME: Convert to IoFilter
 * 
 * @see org.apache.mina.handler.chain.ChainedIoHandler
 */
public class ActiveKeepAliveFilter extends AbstractKeepAliveFilter {
    private final Logger logger = Logger.getLogger(this.getClass());

    private final IoSession session;

    private final ScheduledExecutorService scheduler;

    private final Runnable updateTask;

    private final long pingingInterval; // milliseconds

    private final long waitingTime; // milliseconds

    private long lastPingTime; // milliseconds

    private volatile long lastResponseTime; // milliseconds

    /**
     * Creates a new ActiveKeepAliveFilter with the specified parameters.
     * @param session IoSession to ping and monitor.
     * @param scheduler A scheduler that will run updatetasks when needed.
     * @param pingingInterval time in millis how often to ping the connection
     * @param waitingTime time in millis after which the connection is broken
     * if no replies to pings are received.
     */
    public ActiveKeepAliveFilter(IoSession session,
            ScheduledExecutorService scheduler, long pingingInterval,
            long waitingTime) {
        this.session = session;
        this.scheduler = scheduler;
        this.pingingInterval = pingingInterval;
        this.waitingTime = waitingTime;
        this.updateTask = this.createUpdateTask();

        // give some extra time in the beginning
        this.lastPingTime = System.currentTimeMillis() + pingingInterval;

        this.lastResponseTime = lastPingTime;
        scheduler.schedule(updateTask, pingingInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates the Runnable object that updates this filter at
     * scheduled intervals.
     * @return Runnable update task.
     */
    private Runnable createUpdateTask() {
        return new Runnable() {
            public void run() {
                if (session.isClosing())
                    return;
                final long currentTime = System.currentTimeMillis();

                // check whether to break connection
                long elapsed = currentTime - lastResponseTime;
                if (elapsed >= waitingTime) {
                    logger.info("Ping timeout exceeded, closing connection to "
                            + session.getRemoteAddress());
                    session.close();
                    return;
                }
                // calculate delay to next break time
                long delayToBreak = waitingTime - elapsed + 1;

                //check whether to send ping	NOTE: elapsed is recycled here
                elapsed = currentTime - lastPingTime;
                long delayToPing;
                if (elapsed >= pingingInterval) {
                    lastPingTime = currentTime;
                    delayToPing = pingingInterval;
                    logger.debug("Sending PING");
                    session.write(pingMessage);
                } else {
                    delayToPing = pingingInterval - elapsed + 1;
                }

                // schedule next update
                if (delayToBreak < delayToPing)
                    delayToPing = delayToBreak;
                scheduler.schedule(this, delayToPing, TimeUnit.MILLISECONDS);
            }
        };
    }

    /**
     * Filters the replies to sent ping messages.
     * @see org.apache.mina.handler.chain.IoHandlerCommand
     */
    public void execute(NextCommand next, IoSession session, Object message)
            throws Exception {
        if (pongMessage.equals(message)) {
            if (logger.isDebugEnabled())
                logger.debug("Received pong message from "
                        + session.getRemoteAddress());
            lastResponseTime = System.currentTimeMillis();
        } else
            next.execute(session, message);
    }
}