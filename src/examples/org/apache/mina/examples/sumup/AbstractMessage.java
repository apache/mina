/*
 * @(#) $Id$
 */
package org.apache.mina.examples.sumup;

import java.nio.ByteBuffer;

import net.gleamynode.netty2.Message;
import net.gleamynode.netty2.MessageParseException;

/**
 * A base message for SumUp protocol messages.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractMessage implements Message
{

    private final int type;

    private int sequence;

    private boolean readHeader;

    private boolean wroteHeader;

    protected AbstractMessage( int type )
    {
        this.type = type;
    }

    public int getSequence()
    {
        return sequence;
    }

    public void setSequence( int sequence )
    {
        this.sequence = sequence;
    }

    public final boolean read( ByteBuffer buf ) throws MessageParseException
    {
        // read a header if not read yet.
        if( !readHeader )
        {
            readHeader = readHeader( buf );
            if( !readHeader )
                return false;
        }

        // Header is read, now try to read body
        if( readBody( buf ) )
        {
            // finished reading single complete message
            readHeader = false; // reset state
            return true;
        }
        else
            return false;
    }

    private boolean readHeader( ByteBuffer buf ) throws MessageParseException
    {
        // if header is not fully read, don't read it.
        if( buf.remaining() < Constants.HEADER_LEN )
            return false;

        // read header and validate the message
        int readType = buf.getShort();
        if( type != readType )
            throw new MessageParseException( "type mismatches: " + readType
                                             + " (expected: " + type + ')' );

        // read sequence number of the message
        sequence = buf.getInt();
        return true;
    }

    protected abstract boolean readBody( ByteBuffer buf )
            throws MessageParseException;

    public boolean write( ByteBuffer buf )
    {
        // write a header if not written yet.
        if( !wroteHeader )
        {
            wroteHeader = writeHeader( buf );
            if( !wroteHeader )
                return false; // buffer is almost full perhaps
        }

        // Header is written, now try to write body
        if( writeBody( buf ) )
        {
            // finished writing single complete message
            wroteHeader = false;
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean writeHeader( ByteBuffer buf )
    {
        // check if there is enough space to write header
        if( buf.remaining() < Constants.HEADER_LEN )
            return false;
        buf.putShort( ( short ) type );
        buf.putInt( sequence );
        return true;
    }

    protected abstract boolean writeBody( ByteBuffer buf );
}