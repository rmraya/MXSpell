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

public class DictionaryEntry {

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
            return word.equals(entry.word) && affix.equals(entry.affix);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (word + affix).hashCode();
    }

}
