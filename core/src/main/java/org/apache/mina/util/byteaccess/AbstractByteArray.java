/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.mina.util.byteaccess;


/**
 * 
 * Abstract class that implements {@link ByteArray}.  This class will only be 
 * used internally and should not be used by end users.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
abstract class AbstractByteArray implements ByteArray
{

    /**
     * @inheritDoc
     */
    public final int length()
    {
        return last() - first();
    }


    /**
     * @inheritDoc
     */
    @Override
    public final boolean equals( Object other )
    {
        // Optimization: compare pointers.
        if ( other == this )
        {
            return true;
        }
        // Compare types.
        if ( !( other instanceof ByteArray ) )
        {
            return false;
        }
        ByteArray otherByteArray = ( ByteArray ) other;
        // Compare properties.
        if ( first() != otherByteArray.first() || last() != otherByteArray.last()
            || !order().equals( otherByteArray.order() ) )
        {
            return false;
        }
        // Compare bytes.
        Cursor cursor = cursor();
        Cursor otherCursor = otherByteArray.cursor();
        for ( int remaining = cursor.getRemaining(); remaining > 0; )
        {
            // Optimization: prefer int comparisons over byte comparisons
            if ( remaining >= 4 )
            {
                int i = cursor.getInt();
                int otherI = otherCursor.getInt();
                if ( i != otherI )
                {
                    return false;
                }
            }
            else
            {
                byte b = cursor.get();
                byte otherB = otherCursor.get();
                if ( b != otherB )
                {
                    return false;
                }
            }
        }
        return true;
    }

}
