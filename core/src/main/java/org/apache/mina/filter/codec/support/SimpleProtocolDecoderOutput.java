/**
 * 
 */
package org.apache.mina.filter.codec.support;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFilter.NextFilter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.util.Queue;

/**
 * A {@link ProtocolDecoderOutput} based on queue.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 *
 */
public class SimpleProtocolDecoderOutput implements ProtocolDecoderOutput
{
    private final NextFilter nextFilter;
    private final IoSession session;
    private final Queue messageQueue = new Queue();
    
    public SimpleProtocolDecoderOutput( IoSession session, NextFilter nextFilter )
    {
        this.nextFilter = nextFilter;
        this.session = session;
    }
    
    public void write( Object message )
    {
        messageQueue.push( message );
    }

    public void flush()
    {
        while( !messageQueue.isEmpty() )
        {
            nextFilter.messageReceived( session, messageQueue.pop() );
        }
        
    }
}
