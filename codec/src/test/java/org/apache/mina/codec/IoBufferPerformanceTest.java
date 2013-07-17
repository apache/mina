package org.apache.mina.codec;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.MatcherAssert.assertThat;

public class IoBufferPerformanceTest {

    @Test
    public void testMovingForwardOrNotMovingIsFasterThanMovingBackward() {
        IoBuffer buffer = IoBuffer.wrap(createByteBuffers(10000, 2));

        long forwardsTime = Integer.MAX_VALUE;
        long backwardsTime = Integer.MAX_VALUE;
        long stationaryTime = Integer.MAX_VALUE;
        for (int i = 0; i < 2; i++) { // one warm-up loop before doing the actual measurement

            long start = System.currentTimeMillis();
            timeForwards(buffer);
            long end = System.currentTimeMillis();
            forwardsTime = end - start;

            start = System.currentTimeMillis();
            timeBackwards(buffer);
            end = System.currentTimeMillis();
            backwardsTime = end - start;

            start = System.currentTimeMillis();
            timeStationary(buffer);
            end = System.currentTimeMillis();
            stationaryTime = end - start;
        }

        assertMuchFaster("forwards vs. backwards", forwardsTime, backwardsTime);
        assertMuchFaster("stationary vs. backwards", stationaryTime, backwardsTime);
    }

    private static void timeForwards(IoBuffer buffer) {
        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.position(i);
        }
    }

    private static void timeBackwards(IoBuffer buffer) {
        for (int i = buffer.capacity() - 1; i >= 0; i--) {
            buffer.position(i);
        }
    }

    private static void timeStationary(IoBuffer buffer) {
        int middle = buffer.capacity() / 2;
        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.position(middle);
        }
    }

    private static void assertMuchFaster(String message, long faster, long slower) {
        assertThat(message + ": " + faster + " ms was not much faster than " + slower + " ms",
                faster < (slower / 10));
    }

    private static ByteBuffer[] createByteBuffers(int count, int capacity) {
        ByteBuffer[] buffers = new ByteBuffer[count];
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = ByteBuffer.allocate(capacity);
        }
        return buffers;
    }
}
