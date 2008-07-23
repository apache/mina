package org.apache.mina.example.haiku;

/**
 * @author Apache Mina Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class PhraseUtilities {
    static int countSyllablesInPhrase(String phrase) {
        int syllables = 0;

        String[] words = phrase.split("[^\\w-]+");
        for (int i = 0, max = words.length; i < max; i++) {
            String word = words[i];
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
