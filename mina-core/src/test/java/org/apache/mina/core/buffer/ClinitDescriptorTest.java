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
package org.apache.mina.core.buffer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;


import org.junit.Test;


public class ClinitDescriptorTest {
    static final class ClinitFlags {
        static volatile boolean truncatedProbeInitialized = false;
        static volatile boolean controlProbeInitialized = false;
    }

    public static final class TruncatedProbe implements Serializable {
    private static final long serialVersionUID = 1L;
    
    static { ClinitFlags.truncatedProbeInitialized = true; }
    }

    public static final class ControlProbe implements Serializable {
        private static final long serialVersionUID = 1L;
        static { ClinitFlags.controlProbeInitialized = true; }
    }

    private static byte[] truncatedTypeOneFrame(String className) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(body);
        d.writeShort(0xACED);   // STREAM_MAGIC
        d.writeShort(0x0005);   // STREAM_VERSION
        d.writeByte(0x73);      // TC_OBJECT
        d.writeByte(0x72);      // TC_CLASSDESC
        d.writeByte(0x01);      // Mina type 1 (Serializable)
        d.writeUTF(className);
        // truncated: no super-class descriptor, no field data -> readObject aborts (EOF)

        byte[] b = body.toByteArray();
        ByteArrayOutputStream full = new ByteArrayOutputStream();
        DataOutputStream f = new DataOutputStream(full);
        f.writeInt(b.length); // Mina 4-byte length prefix
        f.write(b);

        return full.toByteArray();
    }

    @Test
    public void truncatedDescriptorMustNotInitializeAllowListedClass() throws Exception {
        assertFalse(ClinitFlags.truncatedProbeInitialized);
        IoBuffer buf =
        IoBuffer.wrap(truncatedTypeOneFrame(TruncatedProbe.class.getName()));
        buf.accept(TruncatedProbe.class.getName()); // allow-listed, so it IS resolved
        
        try {
            buf.getObject();
        } catch (Exception expected) {
        // expected: aborts after the class name
        }
        
        assertFalse("ZDRES-233: <clinit> of an allow-listed class must not run during "
                + "descriptor resolution of an aborted stream", ClinitFlags.truncatedProbeInitialized);
        }

    
    @Test
    public void objectStreamClassLookupInitializesTheClass() {
        assertFalse(ClinitFlags.controlProbeInitialized);
        ObjectStreamClass.lookup(ControlProbe.class);
        assertTrue(ClinitFlags.controlProbeInitialized);
    }
}
