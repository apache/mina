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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class PhraseUtilitiesTest {
    @Test
    public void testCountSyllablesInWord() throws Exception {
        assertSyllableCount(1, "one");
        assertSyllableCount(1, "I");
        assertSyllableCount(1, "too");
        assertSyllableCount(1, "why");
        assertSyllableCount(1, "oh");
        assertSyllableCount(1, "did");
        assertSyllableCount(1, "sign");
        assertSyllableCount(1, "up");
        assertSyllableCount(1, "watch");
        assertSyllableCount(1, "my");
        assertSyllableCount(1, "what");
        assertSyllableCount(1, "is");
        assertSyllableCount(1, "wrong");
        assertSyllableCount(1, "with");
        assertSyllableCount(1, "me");
        assertSyllableCount(1, "don't");
        assertSyllableCount(1, "you");
        assertSyllableCount(1, "love");
        assertSyllableCount(2, "hassle");
        assertSyllableCount(2, "oiling");
        assertSyllableCount(2, "decide");
        assertSyllableCount(2, "Michael");
        assertSyllableCount(1, "I'm");
        assertSyllableCount(1, "check");
        assertSyllableCount(1, "out");
        assertSyllableCount(1, "shirt");
        assertSyllableCount(1, "bitch");
        assertSyllableCount(1, "sucks");
        assertSyllableCount(1, "James");
        assertSyllableCount(2, "ex-wife");
        assertSyllableCount(2, "airlines");
        assertSyllableCount(3, "video");
        assertSyllableCount(3, "fee-ee-ling");
        assertSyllableCount(3, "unbuttoned");
    }

    private static void assertSyllableCount(int count, String word) {
        assertEquals("syllables in " + word, count, PhraseUtilities
                .countSyllablesInWord(word.toLowerCase()));
    }
}
