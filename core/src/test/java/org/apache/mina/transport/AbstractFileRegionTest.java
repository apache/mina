package org.apache.mina.transport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.AvailablePortFinder;

public abstract class AbstractFileRegionTest extends TestCase {

    private static final int FILE_SIZE = 1 * 1024 * 1024; // 1MB file
    
    protected abstract IoAcceptor createAcceptor();
    protected abstract IoConnector createConnector();

    public void testSendLargeFile() throws Throwable {
        File file = createLargeFile();
        assertEquals("Test file not as big as specified", FILE_SIZE, file.length());
        
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final Throwable[] exception = {null};
        
        int port = AvailablePortFinder.getNextAvailable(1025);
        IoAcceptor acceptor = createAcceptor();
        acceptor.setHandler(new IoHandlerAdapter() {
            private int index = 0;
            @Override
            public void exceptionCaught(IoSession session, Throwable cause)
                    throws Exception {
                exception[0] = cause;
                session.close();
            }
            @Override
            public void sessionClosed(IoSession session) throws Exception {
                latch.countDown();
            }
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                IoBuffer buffer = (IoBuffer) message;
                while (buffer.hasRemaining()) {
                    int x = buffer.getInt();
                    if (x != index) {
                        throw new Exception(String.format("Integer at %d was %d but should have been %d", index, x, index));
                    }
                    index++;
                }
                if (index > FILE_SIZE / 4) {
                    throw new Exception("Read too much data");
                }
                if (index == FILE_SIZE / 4) {
                    success[0] = true;
                    session.close();
                }
            }
        });
        acceptor.bind(new InetSocketAddress(port));
        
        IoConnector connector = createConnector();
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void exceptionCaught(IoSession session, Throwable cause)
                    throws Exception {
                exception[0] = cause;
                latch.countDown();
            }
        });
        ConnectFuture future = connector.connect(new InetSocketAddress("localhost", port));
        future.awaitUninterruptibly();
        
        future.getSession().write(file);
        
        latch.await();
        
        if (exception[0] != null) {
            throw exception[0];
        }
        assertTrue("Did not complete file transfer successfully", success[0]);
        
        connector.dispose();
        acceptor.dispose();
    }
    
    private File createLargeFile() throws IOException {
        File largeFile = File.createTempFile("mina-test", "largefile");
        FileChannel channel = new FileOutputStream(largeFile).getChannel();
        ByteBuffer buffer = createBuffer();
        channel.write(buffer);
        channel.close();
        return largeFile;
    }
    private ByteBuffer createBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(FILE_SIZE);
        for (int i = 0; i < FILE_SIZE / 4; i++) {
            buffer.putInt(i);
        }
        buffer.flip();
        return buffer;
    }
    
}
