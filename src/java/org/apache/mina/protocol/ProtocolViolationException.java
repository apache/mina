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
package org.apache.mina.protocol;

import org.apache.mina.common.ByteBuffer;

/**
 * An exception that is thrown when {@link ProtocolEncoder} cannot understand or
 * failed to validate the specified message, or when {@link ProtocolDecoder}
 * cannot understand or failed to validate the specified {@link ByteBuffer}
 * content.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class ProtocolViolationException extends Exception
{
    private static final long serialVersionUID = 3545799879533408565L;

	private String hexdump;

    /**
     * Constructs a new instance.
     */
    public ProtocolViolationException()
    {
    }

    /**
     * Constructs a new instance with the specified message.
     */
    public ProtocolViolationException( String message )
    {
        super( message );
    }

    /**
     * Constructs a new instance with the specified cause.
     */
    public ProtocolViolationException( Throwable cause )
    {
        super( cause );
    }

    /**
     * Constructs a new instance with the specified message and the specified
     * cause.
     */
    public ProtocolViolationException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Returns the message and the hexdump of the unknown part.
     */
    public String getMessage()
    {
        String message = super.getMessage();

        if( message == null )
        {
            message = "";
        }

        if( hexdump != null )
        {
            return message + ( ( message.length() > 0 ) ? " " : "" )
                   + "(Hexdump: " + hexdump + ')';
        }
        else
        {
            return message;
        }
    }

    /**
     * Returns the hexdump of the unknown message part.
     */
    public String getHexdump()
    {
        return hexdump;
    }

    /**
     * Sets the hexdump of the unknown message part.
     */
    public void setHexdump( String hexdump )
    {
        if( this.hexdump != null )
        {
            throw new IllegalStateException( "Hexdump cannot be set more than once." );
        }
        this.hexdump = hexdump;
    }
}