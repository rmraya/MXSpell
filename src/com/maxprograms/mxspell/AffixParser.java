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

    private char[] tryCharacter;
    private String filename;
    private int lineNr;

    private String flagType = ASCII;
    String compoundFlag;
    int compoundMinimalChars = -1;
    private List<String[]> replacementList;
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
                        if (tryCharacter != null) {
                            MessageFormat mf = new MessageFormat("{0}:{1} : duplicate TRY strings: {2}");
                            Object[] args = { filename, "" + lineNr, line };
                            throw new IOException(mf.format(args));
                        }
                        tryCharacter = parts[1].toCharArray();
                        break;
                    case "SET":
                        // ignore, encoding detected before reaching here
                        break;
                    case "COMPOUNDFLAG":
                        if (compoundFlag != null) {
                            MessageFormat mf = new MessageFormat("{0}:{1} : duplicate compound flags: {2}");
                            Object[] args = { filename, "" + lineNr, line };
                            throw new IOException(mf.format(args));
                        }
                        compoundFlag = parts[1];
                        break;
                    case "COMPOUNDMIN":
                        if (compoundMinimalChars >= 0) {
                            MessageFormat mf = new MessageFormat(
                                    "{0}:{1} : duplicate compound minimal char settings: {2}");
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
                    case "MAP":
                        // TODO handle MAP
                        break;
                    case "FLAG":
                        handleFlag(line);
                        break;
                    case "KEY":
                        // TODO handle KEY
                        break;
                    case "WORDCHARS":
                        // TODO handle WORDCHARS
                        break;
                    case "OCONV":
                        // TODO handle OCONV
                        break;
                    case "BREAK":
                        // TODO handle BREAK
                        break;
                    case "FORBIDDENWORD":
                        // TODO handle FORBIDDENWORD
                        break;
                    case "NOSPLITSUGS":
                        // TODO handle NOSPLITSUGS
                        break;
                    case "MAXNGRAMSUGS":
                        // TODO handle MAXNGRAMSUGS
                        break;
                    case "ONLYMAXDIFF":
                        // TODO handle ONLYMAXDIFF
                        break;
                    case "MAXDIFF":
                        // TODO handle MAXDIFF
                        break;
                    case "ICONV":
                        // TODO handle ICONV
                        break;
                    case "FIRST":
                        // TODO handle FIRST
                        break;
                    case "LANG":
                        // TODO handle LANG
                        break;
                    case "NEEDAFFIX":
                        // TODO handle NEEDAFFIX
                        break;
                    case "NOSUGGEST":
                        // TODO handle NOSUGGEST
                        break;
                    case "KEEPCASE":
                        // TODO handle KEEPCASE
                        break;
                    case "COMPOUNDWORDMAX":
                        // TODO handle COMPOUNDWORDMAX
                        break;
                    case "COMPOUNDBEGIN":
                        // TODO handle COMPOUNDBEGIN
                        break;
                    case "COMPOUNDPERMITFLAG":
                        // TODO handle COMPOUNDPERMITFLAG
                        break;
                    case "COMPOUNDMIDDLE":
                        // TODO handle COMPOUNDMIDDLE
                        break;
                    case "COMPOUNDEND":
                        // TODO handle COMPOUNDEND
                        break;
                    case "CHECKCOMPOUNDTRIPLE":
                        // TODO handle CHECKCOMPOUNDTRIPLE
                        break;
                    case "SIMPLIFIEDTRIPLE":
                        // TODO handle SIMPLIFIEDTRIPLE
                        break;
                    case "COMPOUNDRULE":
                        // TODO handle COMPOUNDRULE
                        break;
                    case "COMPOUNDMORESUFFIXES":
                        // TODO handle COMPOUNDMORESUFFIXES
                        break;
                    case "COMPOUNDSYLLABLE":
                        // TODO handle COMPOUNDSYLLABLE
                        break;
                    case "SYLLABLENUM":
                        // TODO handle SYLLABLENUM
                        break;
                    case "ONLYINCOMPOUND":
                        // TODO handle ONLYINCOMPOUND
                        break;
                    case "CHECKCOMPOUNDDUP":
                        // TODO handle CHECKCOMPOUNDDUP
                        break;
                    case "MAXCPDSUGS":
                        // TODO handle MAXCPDSUGS
                        break;
                    case "COMPOUNDROOT":
                        // TODO handle COMPOUNDROOT
                        break;
                    case "HU_KOTOHANGZO":
                        // TODO handle HU_KOTOHANGZO (only in "hu")
                        break;
                    case "GENERATE":
                        // TODO handle GENERATE (only in "hu")
                        break;
                    case "LEMMA_PRESENT":
                        // TODO handle LEMMA_PRESENT (only in "hu")
                        break;
                    case "AF":
                        // TODO handle AF
                        break;
                    case "AM":
                        // TODO handle AM
                        break;
                    case "NAME":
                        // TODO handle NAME
                        break;
                    case "HOME":
                        // TODO handle HOME
                        break;
                    case "VERSION":
                        // TODO handle VERSION
                        break;
                    case "CHECKSHARPS":
                        // TODO handle CHECKSHARPS
                        break;
                    case "CHECKCOMPOUNDREP":
                        // TODO handle CHECKCOMPOUNDREP
                        break;
                    case "COMPOUNDFORBIDFLAG":
                        // TODO handle COMPOUNDFORBIDFLAG
                        break;
                    case "CHECKCOMPOUNDCASE":
                        // TODO handel CHECKCOMPOUNDCASE
                        break;
                    case "CHECKCOMPOUNDPATTERN":
                        // TODO handle CHECKCOMPOUNDPATTERN
                        break;
                    case "COMPOUNDFIRST":
                        // TODO handle COMPOUNDFIRST
                        break;
                    case "COMPOUNDLAST":
                        // TODO hendle COMPOUNDLAST
                        break;
                    case "FULLSTRIP":
                        // TODO handle FULLSTRIP
                        break;
                    case "CIRCUMFIX":
                        // TODO handle CIRCUMFIX
                        break;
                    case "IGNORE":
                        // TODO handle IGNORE
                        break;
                    case "WARN":
                        // TODO handle WARN
                        break;
                    case "FORCEUCASE":
                        // TODO handle FORCEUCASE
                        break;
                    case "LEFTHYPHENMIN":
                        // TODO handle LEFTHYPHENMIN
                        break;
                    case "SUBSTANDARD":
                        // TODO handle SUBSTANDARD
                        break;
                    case "ONLYROOT":
                        // TODO handle ONLYROOT
                        break;
                    default:
                        if (!line.startsWith("#")) {
                            // it does not seem to be a commented-out entry
                            MessageFormat mf = new MessageFormat("{0}:{1} : unknown line: {2}");
                            Object[] args = { filename, "" + lineNr, line };
                            logger.log(Level.WARNING, mf.format(args));
                        }
                }
            }
        }
        if (replacementList != null && replacementSize != replacementList.size()) {
            MessageFormat mf = new MessageFormat("{0} Replacement table size is {0}, expected size:{1}");
            Object[] args = { filename, "" + replacementList.size(), "" + replacementSize };
            throw new IOException(mf.format(args));
        }
        Set<String> keySet = affixMap.keySet();
        Iterator<String> it = keySet.iterator();
        while (it.hasNext()) {
            String key = it.next();
            Affix affix = affixMap.get(key);
            if (!affix.hasAllRules()) {
                MessageFormat mf = new MessageFormat("{0} Incorrect rules number in suffix {1}");
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
            MessageFormat mf = new MessageFormat("{0}:{1} Unupported FLAG type: {2}");
            Object[] args = { filename, "" + lineNr, flagType };
            throw new IOException(mf.format(args));
        }
    }

    private void handleReplacement(String line) throws NumberFormatException {
        String[] parts = line.split("\\s+");
        if (replacementList == null) {
            replacementSize = Integer.parseInt(parts[1]);
            replacementList = new ArrayList<>();
        } else {
            String what = parts[1];
            String replace = parts[2];
            replacementList.add(new String[] { what, replace });
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
                    MessageFormat mf = new MessageFormat("{0}:{1} Unupported affix: {2}");
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

    public String[] getFlags(String affix) throws IOException {
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

    public List<String> getWords(String word, String affixString) throws IOException {
        List<String> result = new ArrayList<>();
        String[] flags = getFlags(affixString);
        for (int i = 0; i < flags.length; i++) {
            String flag = flags[i];
            Affix affix = affixMap.get(flag);
            if (affix == null) {
                MessageFormat mf = new MessageFormat("Unnown affix {0} for word {1}");
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

}
