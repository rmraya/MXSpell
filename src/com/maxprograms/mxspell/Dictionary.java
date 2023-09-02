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
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Dictionary {

    private static final Logger logger = System.getLogger(Dictionary.class.getName());

    private Map<String, DictionaryEntry> wordsMap;
    private AffixParser parser;
    private Set<String> wordsSet;

    public Dictionary(String wordsFile, String affixFile) throws IOException {
        File affixes = new File(affixFile);
        if (!affixes.exists()) {
            MessageFormat mf = new MessageFormat(Messages.getString("Dictionary.0"));
            Object[] args = { affixFile };
            throw new IOException(mf.format(args));
        }
        Charset encoding = EncodingResolver.getEncoding(affixes);
        parser = new AffixParser(affixes, encoding);
        wordsMap = new TreeMap<>();
        wordsSet = new HashSet<>();
        File words = new File(wordsFile);
        if (!words.exists()) {
            MessageFormat mf = new MessageFormat(Messages.getString("Dictionary.1"));
            Object[] args = { wordsFile };
            throw new IOException(mf.format(args));
        }
        loadWords(words, encoding);
    }

    public Dictionary(String zipFile) throws IOException {
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
        File dataFolder = new File(zip.getParentFile(), zipNname.replace('_', '-'));
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
        wordsMap = new TreeMap<>();
        wordsSet = new HashSet<>();
        loadWords(new File(wordsFile), encoding);
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
                StringBuilder builder = new StringBuilder();
                for (int i = 1; i < affixParts.length; i++) {
                    builder.append(' ');
                    builder.append(affixParts[1]);
                }
                wordsMap.put(word,
                        new DictionaryEntry(word, parser.getFlags(affixParts[0]), builder.toString().strip()));
            }
            DictionaryEntry entry = wordsMap.get(word);
            if (entry.getFlags() != null) {
                List<String> words = parser.getWords(word, entry.getFlags());
                wordsSet.addAll(words);
            }
        } else {
            // it's just a word
            wordsMap.put(line, new DictionaryEntry(line, null, null));
            wordsSet.add(line);
        }
    }

    public DictionaryEntry lookup(String word) {
        if (wordsMap.containsKey(word)) {
            return wordsMap.get(word);
        }
        if (wordsSet.contains(word)) {
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
}
