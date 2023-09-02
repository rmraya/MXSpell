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
        if (isCapitalized(word)) {
            entry = dictionary.lookup(word.toLowerCase(locale));
            if (entry != null) {
                return new String[] {};
            }
        }
        List<String> result = new ArrayList<String>();
        Set<String> checkList = new HashSet<>();

        int length = word.length();

        // try removing a char at a time
        for (int i = 0; i < length; i++) {
            StringBuffer candidate = new StringBuffer();
            for (int j = 0; j < length; j++) {
                if (i != j) {
                    candidate.append(word.charAt(j));
                }
            }
            entry = dictionary.lookup(candidate.toString());
            if (entry != null) {
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
                        if (!checkList.contains(candidate)) {
                            result.add(candidate);
                            checkList.add(candidate);
                        }
                    }
                    index = word.indexOf(key, index + 1);
                }
            }
        }
        // try replacing each char with a TRY character
        for (int i = 0; i < tryCharacters.length; i++) {
            for (int j = 0; j < length; j++) {
                StringBuffer candidate = new StringBuffer();
                for (int h = 0; h < length; h++) {
                    if (h != j) {
                        candidate.append(word.charAt(h));
                    } else {
                        candidate.append(tryCharacters[i]);
                    }
                }
                entry = dictionary.lookup(candidate.toString());
                if (entry != null) {
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
                    if (!checkList.contains(candidate.toString())) {
                        result.add(candidate.toString());
                        checkList.add(candidate.toString());
                    }
                }
            }
        }

        // try swapping 2 characters at a time
        for (int i = 0; i < length - 1; i++) {
            StringBuffer candidate = new StringBuffer();
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
        if (word.length() > 3 && word.equals(capitalized(word))) {
            for (int i = 1; i < word.length() - 2; i++) {
                String word1 = capitalized(word.substring(0, i));
                String word2 = capitalized(word.substring(i));
                DictionaryEntry entry1 = dictionary.lookup(word1);
                DictionaryEntry entry2 = dictionary.lookup(word2);
                if (entry1 != null && entry2 != null) {
                    result.add(word1 + " " + word2);
                    checkList.add(word1 + " " + word2);
                }
            }
        }

        return result.toArray(new String[result.size()]);
    }

    private String capitalized(String word) {
        return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }

    private boolean isCapitalized(String word) {
        return word.equals(capitalized(word));
    }
}
