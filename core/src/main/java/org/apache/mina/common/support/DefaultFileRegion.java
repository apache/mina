package org.apache.mina.common.support;

import java.nio.channels.FileChannel;

import org.apache.mina.common.FileRegion;

public class DefaultFileRegion implements FileRegion {

    private final FileChannel channel;
    
    private long originalPosition;
    private long position;
    private long count;
    
    public DefaultFileRegion(FileChannel channel, long position, long count) {
        if (channel == null) {
            throw new IllegalArgumentException("channel can not be null");
        }
        if (position < 0) {
            throw new IllegalArgumentException("position may not be less than 0");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count may not be less than 0");
        }
        this.channel = channel;
        this.originalPosition = position;
        this.position = position;
        this.count = count;
    }
    
    public long getWrittenBytes() {
        return position - originalPosition;
    }

    public long getCount() {
        return count;
    }

    public FileChannel getFileChannel() {
        return channel;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long value) {
        if (value < position) {
            throw new IllegalArgumentException("New position value may not be less than old position value");
        }
        count += value - position;
        position = value;
    }

}
