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
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class EncodingResolver {

    private EncodingResolver() {
        // use getEncoding()
    }

    public static Charset getEncoding(File filename) throws IOException {
        Charset charset = null;
        String declared = "";
        try (FileReader reader = new FileReader(filename)) {
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                String line = "";
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.trim().startsWith("SET ")) {
                        declared = line.trim().substring(4).trim();
                        charset = checkEncoding(declared);
                        break;
                    }
                }
            }
        }
        if (!declared.isEmpty() && charset == null) {
            MessageFormat mf = new MessageFormat("Unsupported dictionary encoding: {0}");
            Object[] args = { declared };
            throw new IOException(mf.format(args));
        }
        if (charset == null) {
            MessageFormat mf = new MessageFormat("SET option not declared in {0}");
            Object[] args = { filename };
            throw new IOException(mf.format(args));
        }
        return charset;
    }

    private static Charset checkEncoding(String string) {
        Map<String, Charset> charsets = new TreeMap<>(Charset.availableCharsets());
        Set<String> keys = charsets.keySet();
        Iterator<String> i = keys.iterator();
        while (i.hasNext()) {
            Charset charset = charsets.get(i.next());
            if (charset.displayName().equalsIgnoreCase(string)) {
                return charset;
            }
        }
        if (string.matches(".*\\d\\d\\d.*")) {
            // Microsoft or ISO based codepage
            int j = 0;
            for (j = 0; !Character.isDigit(string.charAt(j)); j++) {
                // skip non-numeric characteres at the start
            }
            string = string.substring(j);
            i = keys.iterator();
            while (i.hasNext()) {
                Charset charset = charsets.get(i.next());
                if (charset.displayName().indexOf(string) != -1) {
                    return charset;
                }
            }
        }
        return null;
    }
}
