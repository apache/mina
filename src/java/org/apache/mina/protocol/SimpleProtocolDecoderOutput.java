/**
 * 
 */
package org.apache.mina.protocol;

import org.apache.mina.protocol.ProtocolDecoderOutput;
import org.apache.mina.util.Queue;

/**
 * A {@link ProtocolDecoderOutput} based on queue.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 *
 */
public class SimpleProtocolDecoderOutput implements ProtocolDecoderOutput
{
    private final Queue messageQueue = new Queue();
    
    public SimpleProtocolDecoderOutput()
    {
    }
    
    public Queue getMessageQueue()
    {
        return messageQueue;
    }
    
    public void write( Object message )
    {
        messageQueue.push( message );
    }
}