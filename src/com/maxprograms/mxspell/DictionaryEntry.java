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

public class DictionaryEntry implements Comparable<DictionaryEntry> {

    private String word;
    private String[] flags;

    public DictionaryEntry(String word, String[] flags) {
        this.word = word;
        this.flags = flags;
    }

    public String[] getFlags() {
        return flags;
    }

    public String getWord() {
        return word;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DictionaryEntry entry) {
            if (flags != null) {
                return word.equals(entry.word) && flags.equals(entry.flags);
            }
            return word.equals(entry.word) && entry.flags == null;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return flags != null ? (word + flags).hashCode() : word.hashCode();
    }

    @Override
    public int compareTo(DictionaryEntry o) {
        if (!this.equals(o)) {
            int i = word.compareTo(o.word);
            if (i != 0) {
                return i;
            }
            if (flags != null && o.flags != null) {
                return flags.length > o.flags.length ? -1 : 1;
            }
            return flags != null ? -1 : 1;
        }
        return 0;
    }

}
