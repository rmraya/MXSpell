/*******************************************************************************
* Copyright (c) 2023 Maxprograms.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 1.0 which accompanies this distribution,
* and is available at https://www.eclipse.org/org/documents/epl-v10.html
*
* Contributors: Maxprograms - initial API and implementation
*******************************************************************************/
package com.maxprograms.mxspell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SpellCorrector {

    private Dictionary dictionary;
    private Map<String, String> replacement;
    char[] tryCharacters;
    private Locale locale;

    public SpellCorrector(Dictionary dictionary, String language) {
        this.dictionary = dictionary;
        replacement = dictionary.getReplacementMap();
        tryCharacters = dictionary.getTryCharacters();
        locale = new Locale(language);
    }

    public String[] suggest(String word) {
        DictionaryEntry entry = dictionary.lookup(word);
        if (entry != null) {
            return new String[] {};
        }
        if (isUppercase(word)) {
            String lower = word.toLowerCase(locale);
            entry = dictionary.lookup(lower);
            if (entry != null) {
                return new String[] {};
            }
        }
        if (isCapitalized(word)) {
            String lower = word.toLowerCase(locale);
            entry = dictionary.lookup(lower);
            if (entry != null) {
                return new String[] {};
            }
        }

        List<String> result = new ArrayList<>();
        Set<String> checkList = new HashSet<>();
        int length = word.length();

        // try removing a char at a time
        for (int i = 0; i < length; i++) {
            StringBuilder candidate = new StringBuilder();
            for (int j = 0; j < length; j++) {
                if (i != j) {
                    candidate.append(word.charAt(j));
                }
            }
            entry = dictionary.lookup(candidate.toString());
            if (entry != null) {
                try {
                    List<String> words = dictionary.getWords(entry);
                    if (words.contains(word)) {
                        return new String[] {};
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (!checkList.contains(candidate.toString())) {
                    result.add(candidate.toString());
                    checkList.add(candidate.toString());
                }
            }
        }

        // check replacement tables
        if (replacement != null) {
            Set<String> set = replacement.keySet();
            Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                String key = it.next();
                String replace = replacement.get(key);
                int index = word.indexOf(key);
                while (index != -1) {
                    String candidate = word.substring(0, index) + replace + word.substring(index + key.length());
                    entry = dictionary.lookup(candidate);
                    if (entry != null) {
                        try {
                            List<String> words = dictionary.getWords(entry);
                            if (words.contains(word)) {
                                return new String[] {};
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (!checkList.contains(candidate.toString())) {
                            result.add(candidate.toString());
                            checkList.add(candidate.toString());
                        }
                    }
                    index = word.indexOf(key, index + 1);
                }
            }
        }
        // try replacing each char with a TRY character
        for (int i = 0; i < tryCharacters.length; i++) {
            for (int j = 0; j < length; j++) {
                StringBuilder candidate = new StringBuilder();
                for (int h = 0; h < length; h++) {
                    if (h != j) {
                        candidate.append(word.charAt(h));
                    } else {
                        candidate.append(tryCharacters[i]);
                    }
                }
                entry = dictionary.lookup(candidate.toString());
                if (entry != null) {
                    try {
                        List<String> words = dictionary.getWords(entry);
                        if (words.contains(word)) {
                            return new String[] {};
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (!checkList.contains(candidate.toString())) {
                        result.add(candidate.toString());
                        checkList.add(candidate.toString());
                    }
                }
            }
        }

        // try adding a TRY character in front of each letter
        for (int i = 0; i < tryCharacters.length; i++) {
            for (int j = 0; j < length; j++) {
                StringBuffer candidate = new StringBuffer();
                for (int h = 0; h < length; h++) {
                    if (h != j) {
                        candidate.append(word.charAt(h));
                    } else {
                        candidate.append(tryCharacters[i]);
                        candidate.append(word.charAt(h));
                    }
                }
                entry = dictionary.lookup(candidate.toString());
                if (entry != null) {
                    try {
                        List<String> words = dictionary.getWords(entry);
                        if (words.contains(word)) {
                            return new String[] {};
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (!checkList.contains(candidate.toString())) {
                        result.add(candidate.toString());
                        checkList.add(candidate.toString());
                    }
                }
            }
        }

        // try swapping 2 characters at a time
        for (int i = 0; i < length - 1; i++) {
            StringBuilder candidate = new StringBuilder();
            for (int j = 0; j < length; j++) {
                if (i != j) {
                    candidate.append(word.charAt(j));
                } else {
                    candidate.append(word.charAt(j + 1));
                    candidate.append(word.charAt(j));
                    j++;
                }
            }
            entry = dictionary.lookup(candidate.toString());
            if (entry != null) {
                try {
                    List<String> words = dictionary.getWords(entry);
                    if (words.contains(word)) {
                        return new String[] {};
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (!checkList.contains(candidate.toString())) {
                    result.add(candidate.toString());
                    checkList.add(candidate.toString());
                }
            }
        }

        // check if we are dealing with two words
        if (word.length() > 3) {
            for (int i = 1; i < word.length() - 2; i++) {
                String word1 = word.substring(0, i);
                String word2 = word.substring(i);
                DictionaryEntry entry1 = dictionary.lookup(word1);
                DictionaryEntry entry2 = dictionary.lookup(word2);
                if (entry1 != null && entry2 != null) {
                    result.add(word1 + " " + word2);
                    checkList.add(word1 + " " + word2);
                }
            }
        }

        // German dictionary contains capitalized words
        // check for consecutive words keeping this in mind
        if (word.length() > 3 && word.equals(capitalize(word))) {
            for (int i = 1; i < word.length() - 2; i++) {
                String word1 = capitalize(word.substring(0, i));
                String word2 = capitalize(word.substring(i));
                DictionaryEntry entry1 = dictionary.lookup(word1);
                DictionaryEntry entry2 = dictionary.lookup(word2);
                if (entry1 != null && entry2 != null) {
                    result.add(word1 + " " + word2);
                    checkList.add(word1 + " " + word2);
                }
            }
        }

        if (result.size() == 0) {
            // may have a long suffix, try removing chars at the end
            for (int i = length - 1; i > 0; i--) {
                String candidate = word.substring(0, i);
                entry = dictionary.lookup(candidate);
                if (entry != null) {
                    try {
                        List<String> words = dictionary.getWords(entry);
                        if (words.contains(word)) {
                            return new String[] {};
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // may have a long prefix, try removing chars at the beginning
            for (int i = 1; i < length; i++) {
                String candidate = word.substring(i);
                entry = dictionary.lookup(candidate);
                if (entry != null) {
                    try {
                        List<String> words = dictionary.getWords(entry);
                        if (words.contains(word)) {
                            return new String[] {};
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // not found and no suggestions, return current word as result
        return new String[] { word };
    }

    public String capitalize(String word) {
        return word.substring(0, 1).toUpperCase(locale) + word.substring(1).toLowerCase(locale);
    }

    public boolean isCapitalized(String word) {
        return word.equals(capitalize(word));
    }

    public String toLowerCase(String word) {
        return word.toLowerCase(locale);
    }

    public String toUppercase(String word) {
        return word.toUpperCase(locale);
    }

    public boolean isLowercase(String word) {
        return word.equals(word.toLowerCase(locale));
    }

    public boolean isUppercase(String word) {
        return word.equals(word.toUpperCase(locale));
    }

    public boolean isMixedCase(String word) {
        return !isLowercase(word) && !isUppercase(word) && !isCapitalized(word);
    }
}
