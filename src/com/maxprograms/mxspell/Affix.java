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

import java.util.ArrayList;
import java.util.List;

public class Affix {

    public static final String PFX = "PFX";
    public static final String SFX = "SFX";

    private String type;
    private String flag;
    private boolean isCrossProduct;
    private int rulesCount;
    private List<AffixRule> rules;

    public Affix(String type, String flag, String crossProduct, String count) throws NumberFormatException {
        this.type = type;
        this.flag = flag;
        isCrossProduct = "Y".equals(crossProduct);
        this.rulesCount = Integer.parseInt(count);
        rules = new ArrayList<>();
    }

    public String getType() {
        return type;
    }
    
    public void addRule(AffixRule rule) {
        rules.add(rule);
    }

    public List<AffixRule> getRules() {
        return rules;
    }

    public boolean hasAllRules() {
        return rules.size() == rulesCount;
    }

}
