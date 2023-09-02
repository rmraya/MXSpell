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
import java.util.HashMap;
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
            dictionary = makeDictionary(dataFolder);
        }
        if (dictionary == null) {
            dictionaries = loadDictionaryList(dictionaryFolder);
            if (!dictionaries.containsKey(language)) {
                MessageFormat mf = new MessageFormat(Messages.getString("SpellChecker.0"));
                throw new IOException(mf.format(new String[] { language }));
            }
            String zip = dictionaries.get(language);
            dictionary = new Dictionary(zip);
        }
        corrector = new SpellCorrector(dictionary, language);
    }

    public String[] suggest(String word) {
        return corrector.suggest(word);
    }

    public static void main(String[] args) {
        try {
            SpellChecker instance = new SpellChecker("es-ES",
                    "/Users/rmraya/Documents/GitHub/MXSpell/HunspellDictionaries");
            String[] suggestions = instance.suggest("almohadas");
            for (String suggestion : suggestions) {
                System.out.println(suggestion);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Dictionary makeDictionary(File dataFolder) throws IOException {
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
            return new Dictionary(words, affix);
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
}
