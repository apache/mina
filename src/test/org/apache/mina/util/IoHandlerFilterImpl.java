/*
 * @(#) $Id$
 */
package org.apache.mina.util;

import org.apache.mina.io.IoHandlerFilterAdapter;

/**
 * TODO Document me.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class IoHandlerFilterImpl extends IoHandlerFilterAdapter
{
    private final char c;

    public IoHandlerFilterImpl( char c )
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
        if( ! ( o instanceof IoHandlerFilterImpl ) )
            return false;
        return this.c == ( ( IoHandlerFilterImpl ) o ).c;
    }

    public String toString()
    {
        return "" + c;
    }
}