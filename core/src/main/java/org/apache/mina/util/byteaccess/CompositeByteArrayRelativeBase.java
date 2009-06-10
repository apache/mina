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


import java.nio.ByteOrder;

import org.apache.mina.util.byteaccess.ByteArray.Cursor;
import org.apache.mina.util.byteaccess.CompositeByteArray.CursorListener;


/**
 * Provides common functionality between the
 * <code>CompositeByteArrayRelativeReader</code> and
 * <code>CompositeByteArrayRelativeWriter</code>.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
abstract class CompositeByteArrayRelativeBase
{

    /**
     * The underlying <code>CompositeByteArray</code>.
     */
    protected final CompositeByteArray cba;

    /**
     * A cursor of the underlying <code>CompositeByteArray</code>. This
     * cursor is never moved directly; its position only changes through calls
     * to relative read or write methods.
     */
    protected final Cursor cursor;

    /**
     * 
     * Creates a new instance of CompositeByteArrayRelativeBase.
     *
     * @param cba
     *  The {@link CompositeByteArray} that will be the base for this class
     */
    public CompositeByteArrayRelativeBase( CompositeByteArray cba )
    {
        this.cba = cba;
        cursor = cba.cursor( cba.first(), new CursorListener()
        {

            public void enteredFirstComponent( int componentIndex, ByteArray component )
            {
                // Do nothing.
            }


            public void enteredLastComponent( int componentIndex, ByteArray component )
            {
                assert false;
            }


            public void enteredNextComponent( int componentIndex, ByteArray component )
            {
                cursorPassedFirstComponent();
            }


            public void enteredPreviousComponent( int componentIndex, ByteArray component )
            {
                assert false;
            }

        } );
    }


    /**
     * @inheritDoc
     */
    public final int getRemaining()
    {
        return cursor.getRemaining();
    }


    /**
     * @inheritDoc
     */
    public final boolean hasRemaining()
    {
        return cursor.hasRemaining();
    }


    /**
     * @inheritDoc
     */
    public ByteOrder order()
    {
        return cba.order();
    }


    /**
     * Make a <code>ByteArray</code> available for access at the end of this object.
     */
    public final void append( ByteArray ba )
    {
        cba.addLast( ba );
    }


    /**
     * Free all resources associated with this object.
     */
    public final void free()
    {
        cba.free();
    }


    /**
     * Get the index that will be used for the next access.
     */
    public final int getIndex()
    {
        return cursor.getIndex();
    }


    /**
     * Get the index after the last byte that can be accessed.
     */
    public final int last()
    {
        return cba.last();
    }


    /**
     * Called whenever the cursor has passed from the <code>cba</code>'s
     * first component. As the first component is no longer used, this provides
     * a good opportunity for subclasses to perform some action on it (such as
     * freeing it).
     */
    protected abstract void cursorPassedFirstComponent();

}
