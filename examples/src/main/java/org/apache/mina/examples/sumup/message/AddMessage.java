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
package org.apache.mina.examples.sumup.message;

/**
 * <code>ADD</code> message in SumUp protocol.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class AddMessage extends AbstractMessage
{
    private static final long serialVersionUID = -940833727168119141L;

    private int value;

    public AddMessage()
    {
    }

    public int getValue()
    {
        return value;
    }

    public void setValue( int value )
    {
        this.value = value;
    }

    public String toString()
    {
        // it is a good practice to create toString() method on message classes.
        return getSequence() + ":ADD(" + value + ')';
    }
}
