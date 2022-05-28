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

public class Suffix extends Affix {

    @Override
    public boolean checkConditions(String word) {
        int charidx = word.length() - 1;
        for (int cond = numconds - 1; cond >= 0 && charidx >= 0; cond--) {
            try {
                if ((condition[word.charAt(charidx)] & 1 << cond) == 0) {
                    return false;
                }
            } catch (IndexOutOfBoundsException e) {
                return true;
            }
            charidx--;
        }
        return true;
    }
}
