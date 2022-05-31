/*******************************************************************************
* Copyright (c) 2022 Maxprograms.
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
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Dictionary {

    private static final Logger logger = System.getLogger(Dictionary.class.getName());

    public static void main(String[] args) throws IOException {
        Dictionary dictionary = new Dictionary("dictionaries//es_UY.zip");
        System.out.println(dictionary.lookup("vayan"));
    }

    private Map<String, DictionaryEntry> wordsMap;
    private AffixParser parser;

    public Dictionary(String zipFile) throws IOException {
        File zip = new File(zipFile);
        if (!zip.exists()) {
            MessageFormat mf = new MessageFormat("Zip file {0} does not exist");
            Object[] args = { zipFile };
            throw new IOException(mf.format(args));
        }
        String wordsFile = "";
        String affixFile = "";
        try (ZipInputStream input = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry = null;
            while ((entry = input.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(".dic") || name.endsWith(".aff")) {
                    File tmp = new File(zip.getParentFile(), name);
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
            MessageFormat mf = new MessageFormat("Words file is missing in {0}");
            Object[] args = { zipFile };
            throw new IOException(mf.format(args));
        }
        if (affixFile.isEmpty()) {
            MessageFormat mf = new MessageFormat("Affix file is missing in {0}");
            Object[] args = { zipFile };
            throw new IOException(mf.format(args));
        }
        File affixes = new File(affixFile);
        Charset encoding = EncodingResolver.getEncoding(affixes);
        parser = new AffixParser(affixes, encoding);
        wordsMap = new TreeMap<>();
        loadWords(new File(wordsFile), encoding);
    }

    private void loadWords(File words, Charset encoding) throws IOException {
        try (FileReader reader = new FileReader(words, encoding)) {
            try (BufferedReader buffered = new BufferedReader(reader)) {
                int entries = 0;
                String line = buffered.readLine();
                try {
                    entries = Integer.parseInt(line);
                } catch (NumberFormatException nfe) {
                    MessageFormat mf = new MessageFormat("Missing words count in file {0}");
                    Object[] args = { words.getAbsoluteFile() };
                    throw new IOException(mf.format(args));
                }
                while ((line = buffered.readLine()) != null) {
                    int index = line.indexOf("/");
                    if (index > 0) {
                        String word = line.substring(0, index);
                        String affix = line.substring(index + 1);
                        wordsMap.put(word, new DictionaryEntry(word, parser.getFlags(affix)));
                    } else {
                        wordsMap.put(line, new DictionaryEntry(line, null));
                    }
                }
                if (entries != wordsMap.size()) {
                    MessageFormat mf = new MessageFormat("{0}: Expected entries: {1}, entries read: {2}");
                    Object[] args = { words.getName(), "" + entries, "" + wordsMap.size() };
                    logger.log(Level.WARNING, mf.format(args));
                }
            }
        }
    }

    public DictionaryEntry lookup(String word) {
        return wordsMap.get(word);
    }

}
