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
    private String affix;

    public DictionaryEntry(String word, String affix) {
        this.word = word;
        this.affix = affix;
    }

    public String getAffix() {
        return affix;
    }

    public String getWord() {
        return word;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DictionaryEntry entry) {
            if (affix != null) {
                return word.equals(entry.word) && affix.equals(entry.affix);
            }
            return word.equals(entry.word) && entry.affix == null;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return affix != null ? (word + affix).hashCode() : word.hashCode();
    }

    @Override
    public int compareTo(DictionaryEntry o) {
        if (!this.equals(o)) {
            int i = word.compareTo(o.word);
            if (i != 0) {
                return i;
            }
            if (affix != null && o.affix != null) {
                return affix.compareTo(o.affix);
            }
            return affix != null ? -1 : 1;
        }
        return 0;
    }

}
