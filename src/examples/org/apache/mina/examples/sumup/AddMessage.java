/*
 * @(#) $Id$
 */
package org.apache.mina.examples.sumup;

import java.nio.ByteBuffer;

import net.gleamynode.netty2.MessageParseException;

/**
 * <code>ADD</code> message in SumUp protocol.
 *
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class AddMessage extends AbstractMessage
{

    private int value;

    protected AddMessage()
    {
        super( Constants.ADD );
    }

    public int getValue()
    {
        return value;
    }

    public void setValue( int value )
    {
        this.value = value;
    }

    protected boolean readBody( ByteBuffer buf ) throws MessageParseException
    {
        // don't read body if it is partially readable
        if( buf.remaining() < Constants.ADD_BODY_LEN )
            return false;
        value = buf.getInt();
        return true;
    }

    protected boolean writeBody( ByteBuffer buf )
    {
        // check if there is enough space to write body
        if( buf.remaining() < Constants.ADD_BODY_LEN )
            return false;

        buf.putInt( value );

        return true;
    }

    public String toString()
    {
        // it is a good practice to create toString() method on message classes.
        return getSequence() + ":ADD(" + value + ')';
    }
}