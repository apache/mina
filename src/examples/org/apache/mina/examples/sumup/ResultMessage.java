/*
 * @(#) $Id$
 */
package org.apache.mina.examples.sumup;

import java.nio.ByteBuffer;

import net.gleamynode.netty2.MessageParseException;

/**
 * <code>RESULT</code> message in SumUp protocol.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class ResultMessage extends AbstractMessage
{

    private boolean ok;

    private int value;

    private boolean processedResultCode;

    public ResultMessage()
    {
        super( Constants.RESULT );
    }

    public boolean isOk()
    {
        return ok;
    }

    public void setOk( boolean ok )
    {
        this.ok = ok;
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
        if( !processedResultCode )
        {
            processedResultCode = readResultCode( buf );
            if( !processedResultCode )
                return false;
        }

        if( ok )
        {
            if( readValue( buf ) )
            {
                processedResultCode = false;
                return true;
            }
            else
                return false;
        }
        else
        {
            processedResultCode = false;
            return true;
        }
    }

    private boolean readResultCode( ByteBuffer buf )
    {
        if( buf.remaining() < Constants.RESULT_CODE_LEN )
            return false;
        ok = buf.getShort() == Constants.RESULT_OK;
        return true;
    }

    private boolean readValue( ByteBuffer buf )
    {
        if( buf.remaining() < Constants.RESULT_VALUE_LEN )
            return false;
        value = buf.getInt();
        return true;

    }

    protected boolean writeBody( ByteBuffer buf )
    {
        // check if there is enough space to write body
        if( buf.remaining() < Constants.RESULT_CODE_LEN
                              + Constants.RESULT_VALUE_LEN )
            return false;

        buf.putShort( ( short ) ( ok ? Constants.RESULT_OK
                                    : Constants.RESULT_ERROR ) );
        if( ok )
            buf.putInt( value );

        return true;
    }

    public String toString()
    {
        if( ok )
        {
            return getSequence() + ":RESULT(" + value + ')';
        }
        else
        {
            return getSequence() + ":RESULT(ERROR)";
        }
    }
}