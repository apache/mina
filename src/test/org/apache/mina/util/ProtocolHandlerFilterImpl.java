/*
 * @(#) $Id$
 */
package org.apache.mina.util;

import org.apache.mina.protocol.ProtocolHandlerFilterAdapter;

/**
 * TODO Document me.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class ProtocolHandlerFilterImpl extends ProtocolHandlerFilterAdapter
{
    private final char c;

    public ProtocolHandlerFilterImpl( char c )
    {
        this.c = c;
    }

    public int hashCode()
    {
        return c;
    }

    public boolean equals( Object o )
    {
        if( o == null )
            return false;
        if( ! ( o instanceof ProtocolHandlerFilterImpl ) )
            return false;
        return this.c == ( ( ProtocolHandlerFilterImpl ) o ).c;
    }

    public String toString()
    {
        return "" + c;
    }
}