package org.apache.mina.common;

import java.nio.channels.FileChannel;

/**
 * Indicates the region of a file to be sent to the remote host.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 560320 $, $Date: 2007-07-27 11:12:26 -0600 (Fri, 27 Jul 2007) $,
 */
public interface SendFileRegion {

    /**
     * The open <tt>FileChannel<tt> from which data will be read to send to remote host. 
     * 
     * @return  An open <tt>FileChannel<tt>.
     */
    FileChannel getFileChannel();
    
    /**
     * The current file position from which data will be read. 
     * 
     * @return  The current file position.
     */
    long getPosition();
    
    /**
     * Updates the current file position.  May not be negative.
     * 
     * @param value  The new value for the file position.
     */
    void setPosition(long value);
    
    /**
     * The number of bytes to be written from the file to the remote host.
     * 
     * @return  The number of bytes to be written.
     */
    long getCount();
    
    /**
     * The total number of bytes already written.
     * 
     * @return  The total number of bytes already written.
     */
    long getBytesWritten();
    
}
