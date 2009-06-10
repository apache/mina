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
public class PhraseUtilities {
    static int countSyllablesInPhrase(String phrase) {
        int syllables = 0;

        for (String word : phrase.split("[^\\w-]+")) {
            if (word.length() > 0) {
                syllables += countSyllablesInWord(word.toLowerCase());
            }
        }

        return syllables;
    }

    static int countSyllablesInWord(String word) {
        char[] chars = word.toCharArray();
        int syllables = 0;
        boolean lastWasVowel = false;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (isVowel(c)) {
                if (!lastWasVowel
                        || (i > 0 && isE(chars, i - 1) && isO(chars, i))) {
                    ++syllables;
                    lastWasVowel = true;
                }
            } else {
                lastWasVowel = false;
            }
        }

        if (word.endsWith("oned") || word.endsWith("ne")
                || word.endsWith("ide") || word.endsWith("ve")
                || word.endsWith("fe") || word.endsWith("nes")
                || word.endsWith("mes")) {
            --syllables;
        }

        return syllables;
    }

    static boolean isE(char[] chars, int position) {
        return isCharacter(chars, position, 'e');
    }

    static boolean isCharacter(char[] chars, int position, char c) {
        return chars[position] == c;
    }

    static boolean isO(char[] chars, int position) {
        return isCharacter(chars, position, 'o');
    }

    static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u'
                || c == 'y';
    }
}
