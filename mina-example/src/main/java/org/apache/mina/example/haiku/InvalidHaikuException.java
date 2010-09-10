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

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class InvalidHaikuException extends Exception {
    private static final long serialVersionUID = 34877739006797894L;

    private final int position;

    private final String phrase;

    private final int syllableCount;

    private final int expectedSyllableCount;

    public InvalidHaikuException(int position, String phrase,
            int syllableCount, int expectedSyllableCount) {
        super("phrase " + position + ", '" + phrase + "' had " + syllableCount
                + " syllables, not " + expectedSyllableCount);

        this.position = position;
        this.phrase = phrase;
        this.syllableCount = syllableCount;
        this.expectedSyllableCount = expectedSyllableCount;
    }

    public int getExpectedSyllableCount() {
        return expectedSyllableCount;
    }

    public String getPhrase() {
        return phrase;
    }

    public int getSyllableCount() {
        return syllableCount;
    }

    public int getPhrasePosition() {
        return position;
    }
}
