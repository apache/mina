package org.apache.mina.filter.ssl;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.TrustManagerFactory;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Test;

public class SslEngineTest
{
    /** A JVM independant KEY_MANAGER_FACTORY algorithm */
    private static final String KEY_MANAGER_FACTORY_ALGORITHM;

    static {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = KeyManagerFactory.getDefaultAlgorithm();
        }

        KEY_MANAGER_FACTORY_ALGORITHM = algorithm;
    }

    /** App data buffer for the client SSLEngine*/
    private IoBuffer inNetBufferClient;

    /** Net data buffer for the client SSLEngine */
    private IoBuffer outNetBufferClient;

    /** App data buffer for the server SSLEngine */
    private IoBuffer inNetBufferServer;

    /** Net data buffer for the server SSLEngine */
    private IoBuffer outNetBufferServer;
    
    private final IoBuffer emptyBuffer = IoBuffer.allocate(0);


    private static SSLContext createSSLContext() throws IOException, GeneralSecurityException {
        char[] passphrase = "password".toCharArray();

        SSLContext ctx = SSLContext.getInstance("TLS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        ks.load(SslTest.class.getResourceAsStream("keystore.sslTest"), passphrase);
        ts.load(SslTest.class.getResourceAsStream("truststore.sslTest"), passphrase);

        kmf.init(ks, passphrase);
        tmf.init(ts);
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ctx;
    }

    
    /**
     * Decrypt the incoming buffer and move the decrypted data to an
     * application buffer.
     */
    private SSLEngineResult unwrap(SSLEngine sslEngine, IoBuffer inBuffer, IoBuffer outBuffer) throws SSLException {
        // We first have to create the application buffer if it does not exist
        if (outBuffer == null) {
            outBuffer = IoBuffer.allocate(inBuffer.remaining());
        } else {
            // We already have one, just add the new data into it
            outBuffer.expand(inBuffer.remaining());
        }

        SSLEngineResult res;
        Status status;
        HandshakeStatus localHandshakeStatus;

        do {
            // Decode the incoming data
            res = sslEngine.unwrap(inBuffer.buf(), outBuffer.buf());
            status = res.getStatus();

            // We can be processing the Handshake
            localHandshakeStatus = res.getHandshakeStatus();

            if (status == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                // We have to grow the target buffer, it's too small.
                // Then we can call the unwrap method again
                int newCapacity = sslEngine.getSession().getApplicationBufferSize();
                
                if (inBuffer.remaining() >= newCapacity) {
                    // The buffer is already larger than the max buffer size suggested by the SSL engine.
                    // Raising it any more will not make sense and it will end up in an endless loop. Throwing an error is safer
                    throw new SSLException("SSL buffer overflow");
                }

                inBuffer.expand(newCapacity);
                continue;
            }
        } while (((status == SSLEngineResult.Status.OK) || (status == SSLEngineResult.Status.BUFFER_OVERFLOW))
                && ((localHandshakeStatus == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) || 
                        (localHandshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP)));

        return res;
    }

    
    private SSLEngineResult.Status unwrapHandshake(SSLEngine sslEngine, IoBuffer appBuffer, IoBuffer netBuffer) throws SSLException {
        // Prepare the net data for reading.
        if ((appBuffer == null) || !appBuffer.hasRemaining()) {
            // Need more data.
            return SSLEngineResult.Status.BUFFER_UNDERFLOW;
        }

        SSLEngineResult res = unwrap(sslEngine, appBuffer, netBuffer);
        HandshakeStatus handshakeStatus = res.getHandshakeStatus();

        //checkStatus(res);

        // If handshake finished, no data was produced, and the status is still
        // ok, try to unwrap more
        if ((handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED)
                && (res.getStatus() == SSLEngineResult.Status.OK)
                && appBuffer.hasRemaining()) {
            res = unwrap(sslEngine, appBuffer, netBuffer);

            // prepare to be written again
            if (appBuffer.hasRemaining()) {
                appBuffer.compact();
            } else {
                appBuffer.free();
                appBuffer = null;
            }
        } else {
            // prepare to be written again
            if (appBuffer.hasRemaining()) {
                appBuffer.compact();
            } else {
                appBuffer.free();
                appBuffer = null;
            }
        }

        return res.getStatus();
    }

    
    /* no qualifier */boolean isInboundDone(SSLEngine sslEngine) {
        return sslEngine == null || sslEngine.isInboundDone();
    }

    
    /* no qualifier */boolean isOutboundDone(SSLEngine sslEngine) {
        return sslEngine == null || sslEngine.isOutboundDone();
    }

    
    /**
     * Perform any handshaking processing.
     */
    /* no qualifier */HandshakeStatus handshake(SSLEngine sslEngine, IoBuffer appBuffer, IoBuffer netBuffer ) throws SSLException {
        SSLEngineResult result;
        HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();

        for (;;) {
            switch (handshakeStatus) {
            case FINISHED:
                //handshakeComplete = true;
                return handshakeStatus;

            case NEED_TASK:
                //handshakeStatus = doTasks();
                break;

            case NEED_UNWRAP:
                // we need more data read
                SSLEngineResult.Status status = unwrapHandshake(sslEngine, appBuffer, netBuffer);
                handshakeStatus = sslEngine.getHandshakeStatus();

                return handshakeStatus;

            case NEED_WRAP:
                result = sslEngine.wrap(emptyBuffer.buf(), netBuffer.buf());

                while ( result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW ) {
                    netBuffer.capacity(netBuffer.capacity() << 1);
                    netBuffer.limit(netBuffer.capacity());

                    result = sslEngine.wrap(emptyBuffer.buf(), netBuffer.buf());
                }

                netBuffer.flip();
                return result.getHandshakeStatus();

            case NOT_HANDSHAKING:
                result = sslEngine.wrap(emptyBuffer.buf(), netBuffer.buf());

                while ( result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW ) {
                    netBuffer.capacity(netBuffer.capacity() << 1);
                    netBuffer.limit(netBuffer.capacity());

                    result = sslEngine.wrap(emptyBuffer.buf(), netBuffer.buf());
                }

                netBuffer.flip();
                handshakeStatus = result.getHandshakeStatus();
                return handshakeStatus;

            default:
                throw new IllegalStateException("error");
            }
        }
    }

    
    /**
     * Do all the outstanding handshake tasks in the current Thread.
     */
    private SSLEngineResult.HandshakeStatus doTasks(SSLEngine sslEngine) {
        /*
         * We could run this in a separate thread, but I don't see the need for
         * this when used from SSLFilter. Use thread filters in MINA instead?
         */
        Runnable runnable;
        while ((runnable = sslEngine.getDelegatedTask()) != null) {
            //Thread thread = new Thread(runnable);
            //thread.start();
            runnable.run();
        }
        return sslEngine.getHandshakeStatus();
    }
    
    
    private HandshakeStatus handshake(SSLEngine sslEngine, HandshakeStatus expected, 
        IoBuffer inBuffer, IoBuffer outBuffer) throws SSLException {
        HandshakeStatus handshakeStatus = handshake(sslEngine, inBuffer, outBuffer);

        if ( handshakeStatus != expected) {
            fail();
        }
        
        return handshakeStatus;
    }

    
    @Test
    public void testSSL() throws Exception {
        // Initialise the client SSLEngine
        SSLContext sslContextClient = createSSLContext();
        SSLEngine sslEngineClient = sslContextClient.createSSLEngine();
        int packetBufferSize = sslEngineClient.getSession().getPacketBufferSize();
        inNetBufferClient = IoBuffer.allocate(packetBufferSize).setAutoExpand(true);
        outNetBufferClient = IoBuffer.allocate(packetBufferSize).setAutoExpand(true);
        
        sslEngineClient.setUseClientMode(true);

        // Initialise the Server SSLEngine
        SSLContext sslContextServer = createSSLContext();
        SSLEngine sslEngineServer = sslContextServer.createSSLEngine();
        packetBufferSize = sslEngineServer.getSession().getPacketBufferSize();
        inNetBufferServer = IoBuffer.allocate(packetBufferSize).setAutoExpand(true);
        outNetBufferServer = IoBuffer.allocate(packetBufferSize).setAutoExpand(true);
        
        sslEngineServer.setUseClientMode(false);

        HandshakeStatus handshakeStatusClient = sslEngineClient.getHandshakeStatus();
        HandshakeStatus handshakeStatusServer = sslEngineServer.getHandshakeStatus();
        
        // Start the server
        handshakeStatusServer = handshake(sslEngineServer, HandshakeStatus.NEED_UNWRAP, inNetBufferServer, outNetBufferServer);
        
        // Now start the client
        handshakeStatusClient = handshake(sslEngineClient, HandshakeStatus.NEED_UNWRAP, inNetBufferClient, outNetBufferClient);
        
        // 'Read' the CLIENT_HELLO to the server
        handshakeStatusServer = handshake(sslEngineServer, HandshakeStatus.NEED_TASK, outNetBufferClient, outNetBufferServer);

        // Create the SERVER_HELLO message
        handshakeStatusServer = doTasks(sslEngineServer);
        
        // We should get back the message  
        if ( handshakeStatusServer != HandshakeStatus.NEED_WRAP) {
            fail();
        }
        
        // 'Send' the SERVER_HELLO message to the client
        outNetBufferServer.clear();
        handshakeStatusServer = handshake(sslEngineServer, HandshakeStatus.NEED_UNWRAP, null, outNetBufferServer);
        
        // 'Read' the SERVER_HELLO message on the client
        handshakeStatusClient = handshake(sslEngineClient, HandshakeStatus.NEED_TASK, outNetBufferServer, inNetBufferClient);
        
        // Create the  message
        handshakeStatusClient = doTasks(sslEngineClient);
        
        // We should get back the message  
        if ( handshakeStatusClient != HandshakeStatus.NEED_WRAP) {
            fail();
        }
        
        // 'Send' the SERVER_HELLO message to the client
        outNetBufferClient.clear();
        handshakeStatusClient = handshake(sslEngineClient, HandshakeStatus.NEED_WRAP, null, outNetBufferClient);
        
        // 'Send' the CLIENT_KEY_EXCHANGE message to the server
        outNetBufferClient.clear();
        handshakeStatusClient = handshake(sslEngineClient, HandshakeStatus.NEED_WRAP, null, outNetBufferClient);
        
        // 'Send' the ALERT message to the server
        outNetBufferClient.clear();
        handshakeStatusClient = handshake(sslEngineClient, HandshakeStatus.NEED_UNWRAP, null, outNetBufferClient);
    }
}
