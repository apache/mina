/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

import java.net.SocketAddress;

/**
 * A {@link SocketAddress} which represents anonymous in-VM pipe port.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
class AnonymousVmPipeAddress extends SocketAddress implements Comparable
{
    private static final long serialVersionUID = 3258135768999475512L;

    /**
     * Creates a new instance with the specifid port number.
     */
    public AnonymousVmPipeAddress()
    {
    }

    public int hashCode()
    {
        return System.identityHashCode( this );
    }

    public boolean equals( Object o )
    {
	return this == o;
    }

    public int compareTo( Object o )
    {
        return this.hashCode() - ( ( AnonymousVmPipeAddress ) o ).hashCode();
    }

    public String toString()
    {
        return "vm:anonymous(" + hashCode() + ')';
    }
}