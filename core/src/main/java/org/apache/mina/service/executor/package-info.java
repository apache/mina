/**
 * <p>
 * Classes in charge of decoupling IoHandler event of the low level read/write/accept I/O threads ( {@link org.apache.mina.transport.nio.SelectorLoop} ).
 * <p>
 * Two kind of {@link org.apache.mina.service.executor.IoHandlerExecutor} are available :
 * <ul>
 * <li>in order, which will execute events for one session in order (the same thread of the pool will be picked)
 * <li> out of order, which will execute events for one session with no order consideration (can change of thread for events of the same session)
 * </ul>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
package org.apache.mina.service.executor;

