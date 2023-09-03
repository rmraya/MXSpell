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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Dictionary {

    private static final Logger logger = System.getLogger(Dictionary.class.getName());

    private Map<String, DictionaryEntry> wordsMap;
    private AffixParser parser;
    private File dataFolder;
    private List<String> learnedWords;
    private List<String> ignoredWords;

    public Dictionary(String language, String wordsFile, String affixFile) throws IOException {
        File affixes = new File(affixFile);
        if (!affixes.exists()) {
            MessageFormat mf = new MessageFormat(Messages.getString("Dictionary.0"));
            Object[] args = { affixFile };
            throw new IOException(mf.format(args));
        }
        Charset encoding = EncodingResolver.getEncoding(affixes);
        parser = new AffixParser(affixes, encoding);
        Locale locale = new Locale(language);
        wordsMap = new TreeMap<>(Collator.getInstance(locale));
        File words = new File(wordsFile);
        if (!words.exists()) {
            MessageFormat mf = new MessageFormat(Messages.getString("Dictionary.1"));
            Object[] args = { wordsFile };
            throw new IOException(mf.format(args));
        }
        loadWords(words, encoding);
        dataFolder = words.getParentFile();
        loadExceptions();
    }

    public Dictionary(String language, String zipFile) throws IOException {
        File zip = new File(zipFile);
        if (!zip.exists()) {
            MessageFormat mf = new MessageFormat(Messages.getString("Dictionary.2"));
            Object[] args = { zipFile };
            throw new IOException(mf.format(args));
        }
        String zipNname = zip.getName();
        if (zipNname.indexOf('.') != -1) {
            zipNname = zipNname.substring(0, zipNname.lastIndexOf('.'));
        }
        dataFolder = new File(zip.getParentFile(), zipNname.replace('_', '-'));
        if (!dataFolder.exists()) {
            Files.createDirectories(dataFolder.toPath());
        }
        String wordsFile = "";
        String affixFile = "";
        try (ZipInputStream input = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry = null;
            while ((entry = input.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(".dic") || name.endsWith(".aff")) {
                    File tmp = new File(dataFolder, name);
                    if (tmp.exists()) {
                        // remove previous version
                        Files.delete(tmp.toPath());
                    }
                    try (FileOutputStream output = new FileOutputStream(tmp.getAbsolutePath())) {
                        byte[] buf = new byte[2048];
                        int len;
                        while ((len = input.read(buf)) > 0) {
                            output.write(buf, 0, len);
                        }
                    }
                    if (name.endsWith(".dic")) {
                        wordsFile = tmp.getAbsolutePath();
                    }
                    if (name.endsWith(".aff")) {
                        affixFile = tmp.getAbsolutePath();
                    }
                }
            }
        }
        if (wordsFile.isEmpty()) {
            MessageFormat mf = new MessageFormat(Messages.getString("Dictionary.3"));
            Object[] args = { zipFile };
            throw new IOException(mf.format(args));
        }
        if (affixFile.isEmpty()) {
            MessageFormat mf = new MessageFormat(Messages.getString("Dictionary.4"));
            Object[] args = { zipFile };
            throw new IOException(mf.format(args));
        }
        File affixes = new File(affixFile);
        Charset encoding = EncodingResolver.getEncoding(affixes);
        parser = new AffixParser(affixes, encoding);
        Locale locale = new Locale(language);
        wordsMap = new TreeMap<>(Collator.getInstance(locale));
        loadWords(new File(wordsFile), encoding);
        loadExceptions();
    }

    private void loadExceptions() {
        learnedWords = new Vector<>();
        File learnedWordsFile = new File(dataFolder, "learned.txt");
        if (learnedWordsFile.exists()) {
            try (FileReader reader = new FileReader(learnedWordsFile, StandardCharsets.UTF_8)) {
                try (BufferedReader buffered = new BufferedReader(reader)) {
                    String line = "";
                    while ((line = buffered.readLine()) != null) {
                        learnedWords.add(line);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage());
            }
        }
        ignoredWords = new Vector<>();
        File ignoredWordsFile = new File(dataFolder, "ignored.txt");
        if (ignoredWordsFile.exists()) {
            try (FileReader reader = new FileReader(ignoredWordsFile, StandardCharsets.UTF_8)) {
                try (BufferedReader buffered = new BufferedReader(reader)) {
                    String line = "";
                    while ((line = buffered.readLine()) != null) {
                        ignoredWords.add(line);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage());
            }
        }
    }

    public void learn(String word) {
        if (!learnedWords.contains(word)) {
            learnedWords.add(word);
        }
        File learnedWordsFile = new File(dataFolder, "learned.txt");
        try (FileOutputStream output = new FileOutputStream(learnedWordsFile.getAbsolutePath())) {
            for (String w : learnedWords) {
                output.write(w.getBytes(StandardCharsets.UTF_8));
                output.write('\n');
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    public void ignore(String word) {
        if (!ignoredWords.contains(word)) {
            ignoredWords.add(word);
        }
        File ignoredWordsFile = new File(dataFolder, "ignored.txt");
        try (FileOutputStream output = new FileOutputStream(ignoredWordsFile.getAbsolutePath())) {
            for (String w : ignoredWords) {
                output.write(w.getBytes(StandardCharsets.UTF_8));
                output.write('\n');
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    private void loadWords(File words, Charset encoding) throws IOException {
        try (FileReader reader = new FileReader(words, encoding)) {
            try (BufferedReader buffered = new BufferedReader(reader)) {
                int entries = 0;
                String line = buffered.readLine();
                try {
                    // Danish dictionary has comment after word count, remove it
                    String[] result = line.split("\\s#");
                    entries = Integer.parseInt(result[0]);
                } catch (NumberFormatException nfe) {
                    MessageFormat mf = new MessageFormat(Messages.getString("Dictionary.5"));
                    Object[] args = { words.getAbsoluteFile() };
                    logger.log(Level.WARNING, mf.format(args));
                    processWordsLine(line);
                }
                while ((line = buffered.readLine()) != null) {
                    processWordsLine(line);
                }
                if (entries != 0 && entries != wordsMap.size()) {
                    MessageFormat mf = new MessageFormat(Messages.getString("Dictionary.6"));
                    Object[] args = { words.getName(), "" + entries, "" + wordsMap.size() };
                    logger.log(Level.WARNING, mf.format(args));
                }
            }
        }
    }

    private void processWordsLine(String line) throws IOException {
        String[] parts = line.trim().split("\\/");
        if (parts.length > 1) {
            String word = parts[0];
            String affix = parts[1];
            String[] affixParts = affix.split("\\s+");
            if (affixParts.length == 1) {
                // just flags
                wordsMap.put(word, new DictionaryEntry(word, parser.getFlags(affixParts[0]), null));
            } else {
                // contains flags & more
                StringBuffer builder = new StringBuffer();
                for (int i = 1; i < affixParts.length; i++) {
                    builder.append(' ');
                    builder.append(affixParts[1]);
                }
                wordsMap.put(word,
                        new DictionaryEntry(word, parser.getFlags(affixParts[0]), builder.toString().strip()));
            }
        } else {
            // it's just a word
            wordsMap.put(line, new DictionaryEntry(line, null, null));
        }
    }

    public DictionaryEntry lookup(String word) {
        if (wordsMap.containsKey(word)) {
            return wordsMap.get(word);
        }
        if (learnedWords.contains(word)) {
            return new DictionaryEntry(word, null, null);
        }
        if (ignoredWords.contains(word)) {
            return new DictionaryEntry(word, null, null);
        }
        return null;
    }

    public Map<String, String> getReplacementMap() {
        return parser.getReplacementMap();
    }

    public char[] getTryCharacters() {
        return parser.getTryCharacters();
    }

    public List<String> getWords(DictionaryEntry entry) throws IOException {
        String[] flags = entry.getFlags();
        if (flags == null) {
            return new ArrayList<>();
        }
        return parser.getWords(entry.getWord(), flags);
    }
}
