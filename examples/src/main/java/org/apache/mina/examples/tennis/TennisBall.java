/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.examples.tennis;

/**
 * A tennis ball which has TTL value and state whose value is one of 'PING' and
 * 'PONG'.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class TennisBall
{
    private final boolean ping;

    private final int ttl;

    /**
     * Creates a new ball with the specified TTL (Time To Live) value.
     */
    public TennisBall( int ttl )
    {
        this( ttl, true );
    }

    /**
     * Creates a new ball with the specified TTL value and PING/PONG state.
     */
    private TennisBall( int ttl, boolean ping )
    {
        this.ttl = ttl;
        this.ping = ping;
    }

    /**
     * Returns the TTL value of this ball.
     */
    public int getTTL()
    {
        return ttl;
    }

    /**
     * Returns the ball after {@link TennisPlayer}'s stroke.
     * The returned ball has decreased TTL value and switched PING/PONG state.
     */
    public TennisBall stroke()
    {
        return new TennisBall( ttl - 1, !ping );
    }

    /**
     * Returns string representation of this message (<code>[PING|PONG]
     * (TTL)</code>).
     */
    public String toString()
    {
        if( ping )
        {
            return "PING (" + ttl + ")";
        }
        else
        {
            return "PONG (" + ttl + ")";
        }
    }
}