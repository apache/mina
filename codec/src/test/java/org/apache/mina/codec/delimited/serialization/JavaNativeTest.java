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
package org.apache.mina.codec.delimited.serialization;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.apache.mina.codec.delimited.IoBufferDecoder;
import org.apache.mina.codec.delimited.ByteBufferEncoder;

/**
 * A {@link JavaNativeMessageEncoder} and {@link JavaNativeMessageDecoder} test.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class JavaNativeTest extends GenericSerializerTest<JavaNativeTest.TestBean> {
    static public class TestBean implements Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((a == null) ? 0 : a.hashCode());
            result = prime * result + b;
            long temp;
            temp = Double.doubleToLongBits(c);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TestBean other = (TestBean) obj;
            if (a == null) {
                if (other.a != null) {
                    return false;
                }
            } else if (!a.equals(other.a)) {
                return false;
            }
            if (b != other.b) {
                return false;
            }
            if (Double.doubleToLongBits(c) != Double.doubleToLongBits(other.c)) {
                return false;
            }
            return true;
        }

        public TestBean(String a, int b, double c) {
            super();
            this.a = a;
            this.b = b;
            this.c = c;
        }

        private final String a;

        private final int b;

        private final double c;
    }

    @Override
    public List<TestBean> getObjects() {
        List<TestBean> list = new LinkedList<TestBean>();
        list.add(new TestBean("Hello", 86, 12.34));
        list.add(new TestBean("MINA", 94, 67.89));
        return list;
    }

    @Override
    public IoBufferDecoder<TestBean> getDecoder() throws Exception {
        return new JavaNativeMessageDecoder<TestBean>();
    }

    @Override
    public ByteBufferEncoder<TestBean> getEncoder() throws Exception {
        return new JavaNativeMessageEncoder<TestBean>();
    }

}
