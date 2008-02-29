package org.apache.mina.common;

import java.io.IOException;
import java.nio.channels.FileChannel;


public class DefaultFileRegion implements FileRegion {

    private final FileChannel channel;

    private final long originalPosition;
    private long position;
    private long remainingBytes;

    public DefaultFileRegion(FileChannel channel) throws IOException {
        this(channel, 0, channel.size());
    }
    
    public DefaultFileRegion(FileChannel channel, long position, long remainingBytes) {
        if (channel == null) {
            throw new IllegalArgumentException("channel can not be null");
        }
        if (position < 0) {
            throw new IllegalArgumentException("position may not be less than 0");
        }
        if (remainingBytes < 0) {
            throw new IllegalArgumentException("remainingBytes may not be less than 0");
        }
        this.channel = channel;
        this.originalPosition = position;
        this.position = position;
        this.remainingBytes = remainingBytes;
    }

    public long getWrittenBytes() {
        return position - originalPosition;
    }

    public long getRemainingBytes() {
        return remainingBytes;
    }

    public FileChannel getFileChannel() {
        return channel;
    }

    public long getPosition() {
        return position;
    }

    public void update(long value) {
        position += value;
        remainingBytes -= value;
    }

}
