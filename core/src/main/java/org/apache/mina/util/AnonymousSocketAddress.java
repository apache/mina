/*
 * @(#) $Id$
 */
package org.apache.mina.util;

import java.net.SocketAddress;

/**
 * A {@link SocketAddress} which represents anonymous address.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class AnonymousSocketAddress extends SocketAddress implements Comparable
{
    private static final long serialVersionUID = 3978421416766944048L;

    public static final AnonymousSocketAddress INSTANCE = new AnonymousSocketAddress();

    /**
     * Creates a new instance with the specifid port number.
     */
    private AnonymousSocketAddress()
    {
    }

    public int hashCode()
    {
        return 1432482932;
    }

    public boolean equals( Object o )
    {
        if( o == null )
            return false;
        if( this == o )
            return true;
        return o instanceof AnonymousSocketAddress;
    }

    public int compareTo( Object o )
    {
        return this.hashCode() - ( ( AnonymousSocketAddress ) o ).hashCode();
    }

    public String toString()
    {
        return "anonymous";
    }
}