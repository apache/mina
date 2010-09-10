/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.example.haiku;

import java.util.Arrays;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Haiku {
    private final String[] phrases;

    public Haiku(String... lines) {
        this.phrases = lines;
        if (null == lines || lines.length != 3) {
            throw new IllegalArgumentException("Must pass in 3 phrases of text");
        }
    }

    public String[] getPhrases() {
        return phrases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Haiku haiku = (Haiku) o;

        return Arrays.equals(phrases, haiku.phrases);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(phrases);
    }

    @Override
    public String toString() {
        return Arrays.toString(phrases);
    }
}
