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

public class AffixRule {

    private String stripChars;
    private String affix;
    private String condition;

    public AffixRule(String stripChars, String affix, String condition) {
        this.stripChars = stripChars;
        this.affix = affix;
        this.condition = condition;
    }

    public String getStripChars() {
        return stripChars;
    }

    public String getAffix() {
        return affix;
    }

    public String getCondition() {
        return condition;
    }
}
