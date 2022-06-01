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
import java.io.FileFilter;
import java.io.IOException;

public class CheckDictionaries {
    public static void main(String[] args) {
        File folder = new File("dictionaries/");
        File[] dictionaries = folder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".zip");
            }
            
        });
        for (int i=0 ; i<dictionaries.length ; i++) {
            try {
                new Dictionary(dictionaries[i].getAbsolutePath());
            } catch (IOException e) {
                System.err.println(dictionaries[i].getName() + " " +  e.getMessage());
            }
        }
    }
}
