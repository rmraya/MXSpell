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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellChecker {

    private Map<String, String> dictionaries;
    private Dictionary dictionary;
    private SpellCorrector corrector;

    // language must be a valid BCP47 language code
    public SpellChecker(String language, String dictionaryFolder) throws IOException {
        File dataFolder = new File(new File(dictionaryFolder), language);
        if (dataFolder.exists()) {
            if (!dataFolder.isDirectory()) {
                MessageFormat mf = new MessageFormat(Messages.getString("SpellChecker.2"));
                throw new IOException(mf.format(new String[] { dataFolder.getAbsolutePath() }));
            }
            dictionary = makeDictionary(language, dataFolder);
        }
        if (dictionary == null) {
            dictionaries = loadDictionaryList(dictionaryFolder);
            if (!dictionaries.containsKey(language)) {
                MessageFormat mf = new MessageFormat(Messages.getString("SpellChecker.0"));
                throw new IOException(mf.format(new String[] { language }));
            }
            String zip = dictionaries.get(language);
            dictionary = new Dictionary(language, zip);
        }
        corrector = new SpellCorrector(dictionary, language);
    }

    public String[] suggest(String word) {
        String[] suggestions = corrector.suggest(word);
        if (suggestions.length == 1 && suggestions[0].equals(word)) {
            // unknown word, try changing case
            if (checkUppercase(word) || checkCapitalized(word)) {
                // found a lower case version
                return new String[] {};
            }
            if (checkMixedCase(word)) {
                // wrong capitalization
                return new String[] { corrector.capitalize(word), corrector.toLowerCase(word) };
            }
            // try removing a char at a time
            List<String> otherChoices = new ArrayList<>();
            int length = word.length();
            for (int i = 0; i < length; i++) {
                StringBuilder candidateBuilder = new StringBuilder();
                for (int j = 0; j < length; j++) {
                    if (i != j) {
                        candidateBuilder.append(word.charAt(j));
                    }
                }
                String candidate = candidateBuilder.toString();
                if (checkUppercase(candidate)) {
                    String uppercase = corrector.toUppercase(candidate);
                    if (!otherChoices.contains(uppercase)) {
                        otherChoices.add(uppercase);
                    }
                }
                if (checkCapitalized(candidate)) {
                    String capitalized = corrector.capitalize(candidate);
                    if (!otherChoices.contains(capitalized)) {
                        otherChoices.add(capitalized);
                    }
                }
                String[] alternatives = corrector.suggest(candidate);
                if (alternatives.length == 0) {
                    // found a suggestion
                    if (!otherChoices.contains(candidate)) {
                        otherChoices.add(candidate);
                    }
                }
            }
            if (otherChoices.isEmpty()) {
                return suggestions;
            }
            return otherChoices.toArray(new String[otherChoices.size()]);
        }
        return suggestions;
    }

    private boolean checkUppercase(String word) {
        if (corrector.isUppercase(word)) {
            String lower = corrector.toLowerCase(word);
            String[] alternatives = corrector.suggest(lower);
            return alternatives.length == 0;
        }
        return false;
    }

    private boolean checkCapitalized(String word) {
        if (corrector.isCapitalized(word)) {
            String lower = corrector.toLowerCase(word);
            String[] alternatives = corrector.suggest(lower);
            return alternatives.length == 0;
        }
        return false;
    }

    private boolean checkMixedCase(String word) {
        if (corrector.isMixedCase(word)) {
            String lower = corrector.toLowerCase(word);
            String[] alternatives = corrector.suggest(lower);
            return alternatives.length == 0;
        }
        return false;
    }

    public Map<String, String[]> checkString(String text) {
        Map<String, String[]> result = new HashMap<>();
        String[] words = text.split("\s+");
        for (String word : words) {
            char first = word.charAt(0);
            char last = word.charAt(word.length() - 1);
            while (!Character.isLetter(first) && word.length() > 1) {
                word = word.substring(1);
                first = word.charAt(0);
            }
            while (!Character.isLetter(last) && word.length() > 1) {
                word = word.substring(0, word.length() - 1);
                last = word.charAt(word.length() - 1);
            }
            if (word.length() == 1 && !Character.isLetter(first)) {
                continue;
            }
            String[] suggestions = suggest(word);
            if (suggestions.length > 0) {
                result.put(word, suggestions);
            }
        }
        return result;
    }

    private Dictionary makeDictionary(String language, File dataFolder) throws IOException {
        String affix = null;
        String words = null;
        File[] list = dataFolder.listFiles();
        for (int i = 0; i < list.length; i++) {
            if (list[i].getName().endsWith(".aff")) {
                affix = list[i].getAbsolutePath();
            }
            if (list[i].getName().endsWith(".dic")) {
                words = list[i].getAbsolutePath();
            }
        }
        if (affix != null && words != null) {
            return new Dictionary(language, words, affix);
        }
        return null;
    }

    private Map<String, String> loadDictionaryList(String dictionaryFolder) throws IOException {
        Map<String, String> zips = new HashMap<>();
        File folder = new File(dictionaryFolder);
        if (!folder.exists()) {
            MessageFormat mf = new MessageFormat(Messages.getString("SpellChecker.1"));
            throw new IOException(mf.format(new String[] { dictionaryFolder }));
        }
        File[] files = folder.listFiles();
        if (files == null) {
            MessageFormat mf = new MessageFormat(Messages.getString("SpellChecker.2"));
            throw new IOException(mf.format(new String[] { dictionaryFolder }));
        }
        for (File file : files) {
            if (file.isDirectory()) {
                zips.putAll(loadDictionaryList(file.getAbsolutePath()));
                continue;
            }
            if (file.getName().endsWith(".zip")) {
                String name = file.getName().substring(0, file.getName().length() - 4);
                zips.put(name.replace('_', '-'), file.getAbsolutePath());
            }
        }
        return zips;
    }

    public void learn(String word) {
        dictionary.learn(word);
    }

    public void ignore(String word) {
        dictionary.ignore(word);
    }
}
