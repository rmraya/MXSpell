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
import java.util.Set;
import java.util.StringTokenizer;

public class AffixParser {

    Map<String, Affix> prefixMap;
    Map<String, Affix> suffixMap;

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
        suffixMap = new HashMap<>();
        prefixMap = new HashMap<>();

        try (FileReader reader = new FileReader(file, encoding)) {
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                parseAffixFile(bufferedReader);
            }
        }
    }

    Map<String, Affix> getPrefixMap() {
        return prefixMap;
    }

    public Map<String, Affix> getSuffixMap() {
        return suffixMap;
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
                        handleSuffix(line);
                        break;
                    case "PFX":
                        handlePrefix(line);
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
            MessageFormat mf = new MessageFormat("Replacement table size is {0}, expected size:{1}");
            Object[] args = { "" + replacementList.size(), "" + replacementSize };
            throw new IOException(mf.format(args));
        }
        Set<String> keySet = prefixMap.keySet();
        Iterator<String> it = keySet.iterator();
        while (it.hasNext()) {
            String key = it.next();
            Affix affix = prefixMap.get(key);
            if (!affix.hasAllRules()) {
                MessageFormat mf = new MessageFormat("{0} Incorrect rules number in prefix {0}");
                Object[] args = { filename, key };
                throw new IOException(mf.format(args));
            }
        }
        keySet = suffixMap.keySet();
        it = keySet.iterator();
        while (it.hasNext()) {
            String key = it.next();
            Affix affix = suffixMap.get(key);
            if (!affix.hasAllRules()) {
                MessageFormat mf = new MessageFormat("{0} Incorrect rules number in suffix {0}");
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

    private void handlePrefix(String line) throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(line.substring(Affix.PFX.length()));
        String flag = tokenizer.nextToken();
        if (prefixMap.containsKey(flag)) {
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
                    MessageFormat mf = new MessageFormat("{0}:{1} Unupported prefix: {2}");
                    Object[] args = { filename, "" + lineNr, line };
                    throw new IOException(mf.format(args));
                }
            }
            prefixMap.get(flag).addRule(new AffixRule(stripChars, affix, condition));
        } else {
            String crossProduct = tokenizer.nextToken();
            String count = tokenizer.nextToken();
            prefixMap.put(flag, new Affix(Affix.PFX, flag, crossProduct, count));
        }
    }

    private void handleSuffix(String line) throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(line.substring(Affix.SFX.length()));
        String flag = tokenizer.nextToken();
        if (suffixMap.containsKey(flag)) {
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
                    MessageFormat mf = new MessageFormat("{0}:{1} Unupported suffix: {2}");
                    Object[] args = { filename, "" + lineNr, line };
                    throw new IOException(mf.format(args));
                }
            }
            suffixMap.get(flag).addRule(new AffixRule(stripChars, affix, condition));
        } else {
            String crossProduct = tokenizer.nextToken();
            String count = tokenizer.nextToken();
            suffixMap.put(flag, new Affix(Affix.SFX, flag, crossProduct, count));
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

}
