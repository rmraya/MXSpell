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

public abstract class Affix {
    char affix;
    boolean compoundFlag;
    String strip;
    int[] condition;
    int numconds;

    public abstract boolean checkConditions(String word);
}
