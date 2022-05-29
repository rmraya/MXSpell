package com.maxprograms.mxspell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    Map<String, Affix> affixMap;

    private char[] tryCharacter;
    private String filename;
    private int lineNr;

    private String flagType = "ASCII";
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
            if (line.isBlank()) {
                continue;
            }
            StringTokenizer tokenizer = new StringTokenizer(line.trim());
            if (tokenizer.hasMoreTokens()) {
                String tag = tokenizer.nextToken();
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
                        tryCharacter = tokenizer.nextToken().toCharArray();
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
                        compoundFlag = tokenizer.nextToken();
                        break;
                    case "COMPOUNDMIN":
                        if (compoundMinimalChars >= 0) {
                            MessageFormat mf = new MessageFormat(
                                    "{0}:{1} : duplicate compound minimal char settings: {2}");
                            Object[] args = { filename, "" + lineNr, line };
                            throw new IOException(mf.format(args));
                        }
                        compoundMinimalChars = Integer.parseInt(tokenizer.nextToken());
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
                    default:
                        MessageFormat mf = new MessageFormat("{0}:{1} : unknown tag: {2}");
                        Object[] args = { filename, "" + lineNr, line };
                        throw new IOException(mf.format(args));
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
        List<String> validFlags = Arrays.asList("ASCII", "UTF-8", "num", "long");
        if (!validFlags.contains(flagType)) {
            MessageFormat mf = new MessageFormat("{0}:{1} Unupported FLAG type: {2}");
            Object[] args = { filename, "" + lineNr, flagType };
            throw new IOException(mf.format(args));
        }
    }

    private void handleReplacement(String line) throws NumberFormatException {
        StringTokenizer tokenizer = new StringTokenizer(line.substring("REP".length()));
        if (replacementList == null) {
            replacementSize = Integer.parseInt(tokenizer.nextToken());
            replacementList = new ArrayList<>();
        } else {
            String what = tokenizer.nextToken();
            String replace = tokenizer.nextToken();
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

    public String[] getFlags(String affix) {
        if ("UTF-8".equals(flagType) || "ASCII".equals(flagType)) {
            String[] flags = new String[affix.length()];
            for (int i = 0; i < affix.length(); i++) {
                flags[i] = "" + affix.charAt(i);
            }
            return flags;
        }
        if ("long".equals(flagType)) {
            String[] flags = new String[affix.length() / 2];
            for (int i = 0; i < affix.length(); i += 2) {
                flags[i] = "" + affix.charAt(i) + affix.charAt(i + 1);
            }
            return flags;
        }
        if ("num".equals(flagType)) {
            return affix.split(",");
        }
        return null;
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
