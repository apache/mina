/**
 * 
 */
package org.apache.mina.filter.codec.support;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.util.Queue;

/**
 * A {@link ProtocolEncoderOutput} based on queue.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class SimpleProtocolEncoderOutput implements ProtocolEncoderOutput
{
    private final Queue bufferQueue = new Queue();
    
    public SimpleProtocolEncoderOutput()
    {
    }
    
    public Queue getBufferQueue()
    {
        return bufferQueue;
    }
    
    public synchronized void write( ByteBuffer buf )
    {
        bufferQueue.push( buf );
    }
    
    public synchronized void mergeAll()
    {
        int sum = 0;
        final int size = bufferQueue.size();
        
        if( size < 2 )
        {
            // no need to merge!
            return;
        }
        
        // Get the size of merged BB
        for( int i = size - 1; i >= 0; i -- )
        {
            sum += ( ( ByteBuffer ) bufferQueue.get( i ) ).remaining();
        }
        
        // Allocate a new BB that will contain all fragments
        ByteBuffer newBuf = ByteBuffer.allocate( sum );
        
        // and merge all.
        for( ;; )
        {
            ByteBuffer buf = ( ByteBuffer ) bufferQueue.pop();
            if( buf == null )
            {
                break;
            }
    
            newBuf.put( buf );
            buf.release();
        }
        
        // Push the new buffer finally.
        newBuf.flip();
        bufferQueue.push(newBuf);
    }
    
    public synchronized WriteFuture flush()
    {
        Queue bufferQueue = this.bufferQueue;
        WriteFuture future = null;
        if( bufferQueue.isEmpty() )
        {
            future = doFlush( ByteBuffer.allocate( 0 ).flip() ); // write zero bytes
        }
        else
        {
            for( ;; )
            {
                ByteBuffer buf = ( ByteBuffer ) bufferQueue.pop();
                if( buf == null )
                {
                    break;
                }
                
                future = doFlush( buf );
            }
        }
        
        if( future == null )
        {
            throw new IllegalStateException( "doFlush must return future." );
        }
        
        return future;
    }
    
    protected abstract WriteFuture doFlush( ByteBuffer buf );
}