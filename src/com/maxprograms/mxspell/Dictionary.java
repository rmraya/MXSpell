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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Dictionary {

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

    public Dictionary(String wordsFile, String affixFile) {

    }
}
