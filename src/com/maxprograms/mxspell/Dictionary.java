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
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Dictionary {

    private static final Logger logger = System.getLogger(Dictionary.class.getName());

    public static void main(String[] args) throws IOException {
        new Dictionary("dictionaries\\es_UY.zip");
    }

    private Map<String, DictionaryEntry> wordsMap;

    public Dictionary(String zipFile) throws IOException {
        File zip = new File(zipFile);
        if (!zip.exists()) {
            throw new IOException("Zip file does not exist");
        }
        String wordsFile = "";
        String affixFile = "";
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry = null;
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(".dic") || name.endsWith(".aff")) {
                    File tmp = new File(zip.getParentFile(), name);
                    if (tmp.exists()) {
                        Files.delete(tmp.toPath());
                    }
                    try (FileOutputStream output = new FileOutputStream(tmp.getAbsolutePath())) {
                        byte[] buf = new byte[2048];
                        int len;
                        while ((len = in.read(buf)) > 0) {
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
            throw new IOException(".dic file is missing");
        }
        if (affixFile.isEmpty()) {
            throw new IOException(".aff file is missing");
        }
        new Dictionary(wordsFile, affixFile);
    }

    public Dictionary(String wordsFile, String affixFile) throws IOException {
        File words = new File(wordsFile);
        if (!words.exists()) {
            throw new IOException(".dic file is missing");
        }
        File affixes = new File(affixFile);
        if (!affixes.exists()) {
            throw new IOException(".aff file is missing");
        }
        loadWords(words);
        loadAffixes(affixes);
    }

    private void loadWords(File words) throws IOException {
        wordsMap = new HashMap<>();
        try (FileReader reader = new FileReader(words)) {
            try (BufferedReader buffered = new BufferedReader(reader)) {
                int entries = 0;
                String line = buffered.readLine();
                try {
                    entries = Integer.parseInt(line);
                } catch (NumberFormatException nfe) {
                    throw new IOException("Missing word count in .dic file");
                }
                while ((line = buffered.readLine()) != null) {
                    int index = line.indexOf("/");
                    if (index > 0) {
                        String word = line.substring(0, index);
                        String affix = line.substring(index + 1);
                        wordsMap.put(word, new DictionaryEntry(word, affix));
                    } else {
                        wordsMap.put(line, new DictionaryEntry(line, null));
                    }
                }
                if (entries != wordsMap.size()) {
                    logger.log(Level.WARNING, "Expected entries: " + entries + ", entries read: " + wordsMap.size());
                }
            }
        }
    }

    private void loadAffixes(File affixes) {
    }
}
