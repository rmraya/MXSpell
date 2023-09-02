/*******************************************************************************
* Copyright (c) 2023 Maxprograms.
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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

public class AffixParser {

    // Supported FLAG types
    private static final String ASCII = "ASCII";
    private static final String UTF8 = "UTF-8";
    private static final String NUM = "num";
    private static final String LONG = "long";

    private static final Logger logger = System.getLogger(AffixParser.class.getName());
    private Map<String, Affix> affixMap;

    private char[] tryCharacters;
    private String filename;
    private int lineNr;

    private String flagType = ASCII;
    String compoundFlag;
    int compoundMinimalChars = -1;
    private Map<String, String> replacementMap;
    private int replacementSize;

    AffixParser(File file, Charset encoding) throws IOException {
        this.filename = file.getName();
        affixMap = new HashMap<>();
        try (FileReader reader = new FileReader(file, encoding)) {
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                parseAffixFile(bufferedReader);
            }
        }
    }

    public Map<String, Affix> getAffixMap() {
        return affixMap;
    }

    void parseAffixFile(BufferedReader bufferedReader) throws IOException {
        String line = "";
        while ((line = bufferedReader.readLine()) != null) {
            lineNr++;
            line = line.strip();
            String[] parts = line.split("\\s+");
            if (parts.length > 1) {
                String tag = parts[0];
                switch (tag) {
                    case "#":
                        // comment line, ignore
                        break;
                    case "TRY":
                        if (tryCharacters != null) {
                            MessageFormat mf = new MessageFormat(Messages.getString("AffixParser.0"));
                            Object[] args = { filename, "" + lineNr, line };
                            throw new IOException(mf.format(args));
                        }
                        tryCharacters = parts[1].toCharArray();
                        break;
                    case "SET":
                        // ignore, encoding detected before reaching here
                        break;
                    case "COMPOUNDFLAG":
                        if (compoundFlag != null) {
                            MessageFormat mf = new MessageFormat(Messages.getString("AffixParser.1"));
                            Object[] args = { filename, "" + lineNr, line };
                            throw new IOException(mf.format(args));
                        }
                        compoundFlag = parts[1];
                        break;
                    case "COMPOUNDMIN":
                        if (compoundMinimalChars >= 0) {
                            MessageFormat mf = new MessageFormat(Messages.getString("AffixParser.2"));
                            Object[] args = { filename, "" + lineNr, line };
                            throw new IOException(mf.format(args));
                        }
                        compoundMinimalChars = Integer.parseInt(parts[1]);
                        if (compoundMinimalChars < 1 || compoundMinimalChars > 50) {
                            compoundMinimalChars = 3;
                        }
                        break;
                    case "REP":
                        handleReplacement(line);
                        break;
                    case "SFX":
                        handleAffix(Affix.SFX, line);
                        break;
                    case "PFX":
                        handleAffix(Affix.PFX, line);
                        break;
                    case "FLAG":
                        handleFlag(line);
                        break;
                    case "MAP":
                        // handle MAP
                        break;
                    case "KEY":
                        // handle KEY
                        break;
                    case "WORDCHARS":
                        // handle WORDCHARS
                        break;
                    case "OCONV":
                        // handle OCONV
                        break;
                    case "BREAK":
                        // handle BREAK
                        break;
                    case "FORBIDDENWORD":
                        // handle FORBIDDENWORD
                        break;
                    case "NOSPLITSUGS":
                        // handle NOSPLITSUGS
                        break;
                    case "MAXNGRAMSUGS":
                        // handle MAXNGRAMSUGS
                        break;
                    case "ONLYMAXDIFF":
                        // handle ONLYMAXDIFF
                        break;
                    case "MAXDIFF":
                        // handle MAXDIFF
                        break;
                    case "ICONV":
                        // handle ICONV
                        break;
                    case "FIRST":
                        // handle FIRST
                        break;
                    case "LANG":
                        // handle LANG
                        break;
                    case "NEEDAFFIX":
                        // handle NEEDAFFIX
                        break;
                    case "NOSUGGEST":
                        // handle NOSUGGEST
                        break;
                    case "KEEPCASE":
                        // handle KEEPCASE
                        break;
                    case "COMPOUNDWORDMAX":
                        // handle COMPOUNDWORDMAX
                        break;
                    case "COMPOUNDBEGIN":
                        // handle COMPOUNDBEGIN
                        break;
                    case "COMPOUNDPERMITFLAG":
                        // handle COMPOUNDPERMITFLAG
                        break;
                    case "COMPOUNDMIDDLE":
                        // handle COMPOUNDMIDDLE
                        break;
                    case "COMPOUNDEND":
                        // handle COMPOUNDEND
                        break;
                    case "CHECKCOMPOUNDTRIPLE":
                        // handle CHECKCOMPOUNDTRIPLE
                        break;
                    case "SIMPLIFIEDTRIPLE":
                        // handle SIMPLIFIEDTRIPLE
                        break;
                    case "COMPOUNDRULE":
                        // handle COMPOUNDRULE
                        break;
                    case "COMPOUNDMORESUFFIXES":
                        // handle COMPOUNDMORESUFFIXES
                        break;
                    case "COMPOUNDSYLLABLE":
                        // handle COMPOUNDSYLLABLE
                        break;
                    case "SYLLABLENUM":
                        // handle SYLLABLENUM
                        break;
                    case "ONLYINCOMPOUND":
                        // handle ONLYINCOMPOUND
                        break;
                    case "CHECKCOMPOUNDDUP":
                        // handle CHECKCOMPOUNDDUP
                        break;
                    case "MAXCPDSUGS":
                        // handle MAXCPDSUGS
                        break;
                    case "COMPOUNDROOT":
                        // handle COMPOUNDROOT
                        break;
                    case "HU_KOTOHANGZO":
                        // handle HU_KOTOHANGZO (only in "hu")
                        break;
                    case "GENERATE":
                        // handle GENERATE (only in "hu")
                        break;
                    case "LEMMA_PRESENT":
                        // handle LEMMA_PRESENT (only in "hu")
                        break;
                    case "AF":
                        // handle AF
                        break;
                    case "AM":
                        // handle AM
                        break;
                    case "NAME":
                        // handle NAME
                        break;
                    case "HOME":
                        // handle HOME
                        break;
                    case "VERSION":
                        // handle VERSION
                        break;
                    case "CHECKSHARPS":
                        // handle CHECKSHARPS
                        break;
                    case "CHECKCOMPOUNDREP":
                        // handle CHECKCOMPOUNDREP
                        break;
                    case "COMPOUNDFORBIDFLAG":
                        // handle COMPOUNDFORBIDFLAG
                        break;
                    case "CHECKCOMPOUNDCASE":
                        // handel CHECKCOMPOUNDCASE
                        break;
                    case "CHECKCOMPOUNDPATTERN":
                        // handle CHECKCOMPOUNDPATTERN
                        break;
                    case "COMPOUNDFIRST":
                        // handle COMPOUNDFIRST
                        break;
                    case "COMPOUNDLAST":
                        // hendle COMPOUNDLAST
                        break;
                    case "FULLSTRIP":
                        // handle FULLSTRIP
                        break;
                    case "CIRCUMFIX":
                        // handle CIRCUMFIX
                        break;
                    case "IGNORE":
                        // handle IGNORE
                        break;
                    case "WARN":
                        // handle WARN
                        break;
                    case "FORCEUCASE":
                        // handle FORCEUCASE
                        break;
                    case "LEFTHYPHENMIN":
                        // handle LEFTHYPHENMIN
                        break;
                    case "SUBSTANDARD":
                        // handle SUBSTANDARD
                        break;
                    case "ONLYROOT":
                        // handle ONLYROOT
                        break;
                    default:
                        if (!line.startsWith("#")) {
                            // it does not seem to be a commented-out entry
                            MessageFormat mf = new MessageFormat(Messages.getString("AffixParser.3"));
                            Object[] args = { filename, "" + lineNr, line };
                            logger.log(Level.WARNING, mf.format(args));
                        }
                }
            }
        }
        if (replacementMap != null && replacementSize != replacementMap.size()) {
            MessageFormat mf = new MessageFormat(Messages.getString("AffixParser.4"));
            Object[] args = { filename, "" + replacementMap.size(), "" + replacementSize };
            throw new IOException(mf.format(args));
        }
        Set<String> keySet = affixMap.keySet();
        Iterator<String> it = keySet.iterator();
        while (it.hasNext()) {
            String key = it.next();
            Affix affix = affixMap.get(key);
            if (!affix.hasAllRules()) {
                MessageFormat mf = new MessageFormat(Messages.getString("AffixParser.5"));
                Object[] args = { filename, key };
                throw new IOException(mf.format(args));
            }
        }
    }

    private void handleFlag(String line) throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(line.substring("FLAG".length()));
        flagType = tokenizer.nextToken();
        List<String> validFlags = Arrays.asList(ASCII, UTF8, NUM, LONG);
        if (!validFlags.contains(flagType)) {
            MessageFormat mf = new MessageFormat(Messages.getString("AffixParser.6"));
            Object[] args = { filename, "" + lineNr, flagType };
            throw new IOException(mf.format(args));
        }
    }

    private void handleReplacement(String line) throws NumberFormatException {
        String[] parts = line.split("\\s+");
        if (replacementMap == null) {
            replacementSize = Integer.parseInt(parts[1]);
            replacementMap = new HashMap<>();
        } else {
            String what = parts[1];
            String replace = parts[2];
            replacementMap.put(what, replace);
        }
    }

    private void handleAffix(String type, String line) throws IOException, NoSuchElementException {
        StringTokenizer tokenizer = new StringTokenizer(line.substring(type.length()));
        String flag = tokenizer.nextToken();
        if (affixMap.containsKey(flag)) {
            String stripChars = tokenizer.nextToken();
            String affix = tokenizer.nextToken();
            String condition = "";
            if (tokenizer.hasMoreTokens()) {
                condition = tokenizer.nextToken();
            } else {
                if (".".equals(affix)) {
                    affix = "";
                    condition = ".";
                } else {
                    MessageFormat mf = new MessageFormat(Messages.getString("AffixParser.7"));
                    Object[] args = { filename, "" + lineNr, line };
                    throw new IOException(mf.format(args));
                }
            }
            affixMap.get(flag).addRule(new AffixRule(stripChars, affix, condition));
        } else {
            String crossProduct = tokenizer.nextToken();
            String count = tokenizer.nextToken();
            affixMap.put(flag, new Affix(type, flag, crossProduct, count));
        }
    }

    public String[] getFlags(String affix) {
        if (UTF8.equals(flagType) || ASCII.equals(flagType)) {
            String[] flags = new String[affix.length()];
            for (int i = 0; i < affix.length(); i++) {
                flags[i] = "" + affix.charAt(i);
            }
            return flags;
        }
        if (LONG.equals(flagType)) {
            String[] flags = new String[affix.length() / 2];
            for (int i = 0; i < affix.length() / 2; i += 2) {
                flags[i] = "" + affix.charAt(i) + affix.charAt(i + 1);
            }
            return flags;
        }
        if (NUM.equals(flagType)) {
            return affix.split(",");
        }
        return new String[] {};
    }

    public List<String> getWords(String word, String[] flags) throws IOException {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < flags.length; i++) {
            String flag = flags[i];
            Affix affix = affixMap.get(flag);
            if (affix == null) {
                MessageFormat mf = new MessageFormat(Messages.getString("AffixParser.8"));
                Object[] args = { flag, word };
                throw new IOException(mf.format(args));
            }
            List<String> processed = processRules(affix.getType(), word, affix.getRules());
            result.addAll(processed);
        }
        return result;
    }

    private List<String> processRules(String type, String word, List<AffixRule> rules) {
        List<String> result = new ArrayList<>();
        Iterator<AffixRule> it = rules.iterator();
        while (it.hasNext()) {
            AffixRule rule = it.next();
            String condition = rule.getCondition();
            if (matchesCondition(type, word, condition)) {
                String stripped = word;
                String stripChars = rule.getStripChars();
                if (!"0".equals(stripChars)) {
                    int length = stripChars.length();
                    if (Affix.PFX.equals(type)) {
                        stripped = word.substring(length);
                    } else {
                        stripped = stripped.substring(0, stripped.length() - length);
                    }
                }
                if (Affix.PFX.equals(type)) {
                    stripped = rule.getAffix() + stripped;
                } else {
                    stripped = stripped + rule.getAffix();
                }
                if (!result.contains(stripped)) {
                    result.add(stripped);
                }
            }
        }
        return result;
    }

    private boolean matchesCondition(String type, String word, String condition) {
        if (".".equals(condition)) {
            return true;
        }
        String regex = type.equals(Affix.PFX) ? "^" + condition + ".*" : ".*" + condition + "$";
        return word.matches(regex);
    }

    public Map<String, String> getReplacementMap() {
        return replacementMap;
    }

    public char[] getTryCharacters() {
        return tryCharacters;
    }

}
