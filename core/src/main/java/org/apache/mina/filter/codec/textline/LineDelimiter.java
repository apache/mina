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
package org.apache.mina.filter.codec.textline;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
 * A delimiter which is appended to the end of a text line, such as
 * <tt>CR/LF</tt>.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class LineDelimiter {
    /**
     * the line delimiter constant of the current O/S.
     */
    public static final LineDelimiter DEFAULT;

    static {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(bout);
        out.println();
        DEFAULT = new LineDelimiter(new String(bout.toByteArray()));
    }

    /**
     * A special line delimiter which is used for auto-detection of
     * EOL in {@link TextLineDecoder}.  If this delimiter is used,
     * {@link TextLineDecoder} will consider both  <tt>'\r'</tt> and
     * <tt>'\n'</tt> as a delimiter. 
     */
    public static final LineDelimiter AUTO = new LineDelimiter("");

    /**
     * The line delimiter constant of UNIX (<tt>"\n"</tt>)
     */
    public static final LineDelimiter UNIX = new LineDelimiter("\n");

    /**
     * The line delimiter constant of MS Windows/DOS (<tt>"\r\n"</tt>)
     */
    public static final LineDelimiter WINDOWS = new LineDelimiter("\r\n");

    /**
     * The line delimiter constant of Mac OS (<tt>"\r"</tt>)
     */
    public static final LineDelimiter MAC = new LineDelimiter("\r");

    private final String value;

    /**
     * Creates a new line delimiter with the specified <tt>value</tt>.
     */
    public LineDelimiter(String value) {
        if (value == null) {
            throw new NullPointerException("delimiter");
        }
        this.value = value;
    }

    /**
     * Return the delimiter string.
     */
    public String getValue() {
        return value;
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof LineDelimiter)) {
            return false;
        }

        LineDelimiter that = (LineDelimiter) o;
        return this.value.equals(that.value);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("delimiter:");
        if (value.length() == 0) {
            buf.append(" auto");
        } else {
            for (int i = 0; i < value.length(); i++) {
                buf.append(" 0x");
                buf.append(Integer.toHexString(value.charAt(i)));
            }
        }
        return buf.toString();
    }
}
