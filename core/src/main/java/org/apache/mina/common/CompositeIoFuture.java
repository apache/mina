package org.apache.mina.common;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An {@link IoFuture} of {@link IoFuture}s.  It is useful when you want to
 * get notified when all {@link IoFuture}s are complete.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 *
 * @param <E> the type of the child futures.
 */
public class CompositeIoFuture<E extends IoFuture> extends DefaultIoFuture {
    
    private final NotifyingListener listener = new NotifyingListener();
    private final AtomicInteger unnotified = new AtomicInteger();
    private volatile boolean constructionFinished;
    
    public CompositeIoFuture(Iterable<E> children) {
        super(null);
        
        for (E f: children) {
            f.addListener(listener);
            unnotified.incrementAndGet();
        }
        
        constructionFinished = true;
        if (unnotified.get() == 0) {
            setValue(true);
        }
    }
    
    private class NotifyingListener implements IoFutureListener<IoFuture> {
        public void operationComplete(IoFuture future) {
            if (unnotified.decrementAndGet() == 0 && constructionFinished) {
                setValue(true);
            }
        }
    }
}
