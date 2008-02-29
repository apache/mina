package org.apache.mina.filter.stream;

import java.io.IOException;

import org.apache.mina.common.FileRegion;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;

/**
 * Filter implementation that converts a {@link FileRegion} to {@link IoBuffer}
 * objects and writes those buffers to the next filter. When end of the
 * {@code FileRegion} has been reached this filter will call
 * {@link IoFilter.NextFilter#messageSent(IoSession,WriteRequest)} using the
 * original {@link FileRegion} written to the session and notifies
 * {@link org.apache.mina.common.WriteFuture} on the original
 * {@link org.apache.mina.common.WriteRequest}.
 * <p>Normall {@code FileRegion} objects should be handled by the
 * {@link org.apache.mina.common.IoProcessor} but this is not always possible
 * if a filter is being used that needs to modify the contents of the file
 * before sending over the network (i.e. the
 * {@link org.apache.mina.filter.ssl.SslFilter} or a data compression filter.)
 * </p>
 * <p> This filter will ignore written messages which aren't {@link FileRegion}
 * instances. Such messages will be passed to the next filter directly.
 * </p>
 * <p><b>NOTE:</b> this filter does not close the file channel in
 * {@link FileRegion#getFileChannel()} after the data from the file has been
 * written.  The {@code FileChannel} should be closed in either 
 * {@link org.apache.mina.common.IoHandler#messageSent(IoSession,Object)}
 * or in an {@link org.apache.mina.common.IoFutureListener} associated with the
 * {@code WriteFuture}.
 * </p>
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class FileRegionWriteFilter extends
        AbstractStreamWriteFilter<FileRegion> {

    @Override
    protected Class<FileRegion> getMessageClass() {
        return FileRegion.class;
    }

    @Override
    protected IoBuffer getNextBuffer(FileRegion fileRegion) throws IOException {
        // If there are no more bytes to read, return null
        if (fileRegion.getRemainingBytes() <= 0) {
            return null;
        }
        
        // Allocate the buffer for reading from the file
        final int bufferSize = (int) Math.min(getWriteBufferSize(), fileRegion.getRemainingBytes());
        IoBuffer buffer = IoBuffer.allocate(bufferSize);

        // Read from the file
        int bytesRead = fileRegion.getFileChannel().read(buffer.buf(),
                fileRegion.getPosition());
        fileRegion.update(bytesRead);

        // return the buffer
        buffer.flip();
        return buffer;
    }

}
