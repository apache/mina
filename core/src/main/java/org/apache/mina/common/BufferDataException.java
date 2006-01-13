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
package org.apache.mina.common;

/**
 * A {@link RuntimeException} which is thrown when the data the {@link ByteBuffer}
 * contains is corrupt.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 *
 */
public class BufferDataException extends RuntimeException
{
    private static final long serialVersionUID = -4138189188602563502L;

    public BufferDataException()
    {
        super();
    }

    public BufferDataException( String message )
    {
        super( message );
    }

    public BufferDataException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public BufferDataException( Throwable cause )
    {
        super( cause );
    }

}
