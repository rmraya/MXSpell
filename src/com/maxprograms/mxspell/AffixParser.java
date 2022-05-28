package com.maxprograms.mxspell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class AffixParser {

    public static final int SETSIZE = 2048; // 1024
    public static final int MAXLNLEN = SETSIZE;

    Map<String, List<Prefix>> prefixMap;
    Map<String, List<Suffix>> suffixMap;

    char[] tryCharacter;
    int[] mbr = new int[MAXLNLEN];
    private String filename;

    private String line;
    private int lineNr;
    private StringTokenizer tokenizer;
    String compoundFlag;
    int compoundMinimalChars = -1;
    Map<String, String> replacement;

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

    Map<String, List<Prefix>> getPrefixMap() {
        return prefixMap;
    }

    public Map<String, List<Suffix>> getSuffixMap() {
        return suffixMap;
    }

    String parseSingleValueLine(String tagname) throws IOException {
        if (!tokenizer.hasMoreTokens()) {
            MessageFormat mf = new MessageFormat("{0}:{1} : missing {2} information: {3}");
            Object[] args = { filename, "" + lineNr, tagname, line };
            throw new IOException(mf.format(args));
        }
        String result = tokenizer.nextToken();
        if (tokenizer.hasMoreTokens()) {
            MessageFormat mf = new MessageFormat("{0}:{1} : extra token: {2}");
            Object[] args = { filename, "" + lineNr, line };
            throw new IOException(mf.format(args));
        }
        return result;
    }

    void parseAffixFile(BufferedReader bufferedReader) throws IOException {
        while ((line = bufferedReader.readLine())!= null) {
            lineNr++;
            if (line.isBlank()) {
                continue;
            }
            tokenizer = new StringTokenizer(line.trim());
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
                        tryCharacter = parseSingleValueLine("TRY").toCharArray();
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
                        compoundFlag = parseSingleValueLine("COMPOUNDFLAG");
                        break;
                    case "COMPOUNDMIN":
                        if (compoundMinimalChars >= 0) {
                            MessageFormat mf = new MessageFormat(
                                    "{0}:{1} : duplicate compound minimal char settings: {2}");
                            Object[] args = { filename, "" + lineNr, line };
                            throw new IOException(mf.format(args));
                        }
                        compoundMinimalChars = Integer.parseInt(parseSingleValueLine("COMPOUNDMIN"));
                        if (compoundMinimalChars < 1 || compoundMinimalChars > 50) {
                            compoundMinimalChars = 3;
                        }
                        break;
                    case "REP":
                        if (replacement != null) {
                            MessageFormat mf = new MessageFormat("{0}:{1} : duplicate REP tables: {2}");
                            Object[] args = { filename, "" + lineNr, line };
                            throw new IOException(mf.format(args));
                        }
                        int replacementCount = Integer.parseInt(parseSingleValueLine("REP"));
                        if (replacementCount < 1) {
                            MessageFormat mf = new MessageFormat(
                                    "{0}:{1} : incorrect number of entries in replacement table: {2}");
                            Object[] args = { filename, "" + lineNr, line };
                            throw new IOException(mf.format(args));
                        }
                        replacement = new HashMap<>(replacementCount);
                        for (int j = 0; j < replacementCount; j++) {
                            line = bufferedReader.readLine();
                            lineNr++;
                            if (line == null) {
                                MessageFormat mf = new MessageFormat(
                                        "{0}:{1} : unexpected end of file while reading replacement table: {2}");
                                Object[] args = { filename, "" + lineNr, line };
                                throw new IOException(mf.format(args));
                            }
                            tokenizer = new StringTokenizer(line.trim());
                            if (tokenizer.hasMoreTokens()) {
                                if (!tokenizer.nextToken().equals("REP")) {
                                    MessageFormat mf = new MessageFormat(
                                            "{0}:{1} : replacement table is corrupt: {2}");
                                    Object[] args = { filename, "" + lineNr, line };
                                    throw new IOException(mf.format(args));
                                }
                                if (tokenizer.hasMoreTokens()) {
                                    String pattern = tokenizer.nextToken();
                                    if (tokenizer.hasMoreTokens()) {
                                        replacement.put(pattern, tokenizer.nextToken());
                                        // Ignore extra tokens. there are comments and
                                        // samples in hu_HU
                                        /*
                                         * if (tok.hasMoreTokens()) {
                                         * throw new IOException(filename + ':' + lineNr + ": extra token: "
                                         * + line);
                                         * }
                                         */
                                    }
                                }
                            } else {
                                MessageFormat mf = new MessageFormat(
                                        "{0}:{1} : missing pattern/replacement: {2}");
                                Object[] args = { filename, "" + lineNr, line };
                                throw new IOException(mf.format(args));
                            }
                        }
                        break;
                    case "SFX":
                        parseAffix(bufferedReader, "SFX", suffixMap);
                        break;
                    case "PFX":
                        parseAffix(bufferedReader, "PFX", prefixMap);
                        break;
                    case "MAP":
                        // TODO handle MAP
                        break;
                    case "FLAG":
                        // TODO handle FLAG
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
    }

    private void parseAffix(BufferedReader bufferedReader, String tagname, Map map) throws IOException {
        if (!tokenizer.hasMoreTokens()) {
            MessageFormat mf = new MessageFormat("{0}:{1} : missing {2} information: {3}");
            Object[] args = { filename, "" + lineNr, tagname, line };
            throw new IOException(mf.format(args));
        }
        String affixChar = tokenizer.nextToken();
        if (affixChar.length() > 1) {
            MessageFormat mf = new MessageFormat("{0}:{1} : affix char too long: {2}");
            Object[] args = { filename, "" + lineNr, line };
            throw new IOException(mf.format(args));
        }
        String compoundString = tokenizer.nextToken();
        if (compoundString.length() > 1 || compoundString.charAt(0) != 'Y' && compoundString.charAt(0) != 'N') {
            MessageFormat mf = new MessageFormat("{0}:{1} : unknown compound indicator ''{2}'' in: {3}");
            Object[] args = { filename, "" + lineNr, compoundString, line };
            throw new IOException(mf.format(args));
        }
        // if (piece.charAt(0) == 'Y')
        // ff = XPRODUCT;
        boolean compoundflag = compoundString.charAt(0) == 'Y';
        int entryCount = Integer.parseInt(tokenizer.nextToken());
        // Ignore extra tokens. They are present at least on es_ES
        /*
         * if (tok.hasMoreTokens()) { throw new IOException(filename +
         * ':' + lineNr + ": extra token: " + line); }
         */
        for (int i = 0; i < entryCount; i++) {
            line = bufferedReader.readLine();
            lineNr++;
            if (line == null) {
                MessageFormat mf = new MessageFormat("{0}:{1} : unexpected end of file while reading {2}: {3}");
                Object[] args = { filename, "" + lineNr, tagname, line };
                throw new IOException(mf.format(args));
            }
            tokenizer = new StringTokenizer(line.trim());
            if (tokenizer.hasMoreTokens()) {
                if (!tokenizer.nextToken().equals(tagname)) {
                    MessageFormat mf = new MessageFormat("{0}:{1} : entry is corrupt, expected ''{2}'': {3}");
                    Object[] args = { filename, "" + lineNr, tagname, line };
                    throw new IOException(mf.format(args));
                }
                String localAffixChar = tokenizer.nextToken();
                if (!localAffixChar.equals(affixChar)) {
                    MessageFormat mf = new MessageFormat("{0}:{1} : entry is corrupt,expected ''{2}'': {3}");
                    Object[] args = { filename, "" + lineNr, affixChar, line };
                    throw new IOException(mf.format(args));
                }
                Affix a;
                if (map == suffixMap) {
                    a = new Suffix();
                } else {
                    a = new Prefix();
                }
                a.affix = affixChar.charAt(0);
                a.compoundFlag = compoundflag;
                // 3 - is string to strip or 0 for null
                a.strip = tokenizer.nextToken();
                String append = tokenizer.nextToken();
                if (a.strip.equals("0")) {
                    a.strip = "";
                }
                Map it = map;
                if (!append.equals("0")) {
                    if (map == suffixMap) {
                        append = new StringBuffer(append).reverse().toString();
                    }
                    int j = 0;
                    Map next = (Map) it.get(append.substring(j, j + 1));
                    j++;
                    while (next != null && j < append.length()) {
                        it = next;
                        next = (Map) it.get(append.substring(j, j + 1));
                        j++;
                    }
                    if (next == null) {
                        j--;
                        while (j < append.length()) {
                            Map temp = new HashMap();
                            it.put(append.substring(j, j + 1), temp);
                            it = temp;
                            j++;
                        }
                    } else {
                        it = next;
                    }
                }
                List l = (List) it.get("");
                if (l == null) {
                    l = new LinkedList();
                    it.put("", l);
                }
                l.add(a);
                if (tokenizer.hasMoreTokens()) {
                    String condition = tokenizer.nextToken();
                    encodeit(a, condition);
                } else {
                    // some entries are broken in ru_RU
                    encodeit(a, "");
                }
                // Ignore extra tokens. They are present as example
                // in hu_HU
                /*
                 * if (tok.hasMoreTokens()) { throw new
                 * IOException(filename + ':' + lineNr + ": extra token: " +
                 * line); }
                 */
            }
        }
    }

    private void encodeit(Affix ptr, String cs) {

        // now parse the string to create the conds array */
        int nc = cs.length();

        // if no condition just return
        if (cs.equals(".")) {
            ptr.condition = null;
            return;
        }

        ptr.condition = new int[SETSIZE];
        // now clear the conditions array */
        for (int i = 0; i < SETSIZE; i++) {
            ptr.condition[i] = 0;
        }

        int n = 0; // number of conditions
        int i = 0;
        while (i < nc) {
            char c = cs.charAt(i);
            if (c == '[') {
                // start group
                i++;
                c = cs.charAt(i);
                boolean neg = false;
                if (c == '^') {
                    // negated
                    neg = true;
                    i++;
                    c = cs.charAt(i);
                }
                int nm = 0;
                while (c != ']' && i < cs.length() - 1) {
                    mbr[nm] = c;
                    nm++;
                    i++;
                    c = cs.charAt(i);
                }
                if (neg) {
                    // complement so set all of them and then unset indicated ones
                    for (int j = 0; j < SETSIZE; j++) {
                        ptr.condition[j] = ptr.condition[j] | 1 << n;
                    }
                    for (int j = 0; j < nm; j++) {
                        int k = mbr[j];
                        ptr.condition[k] = ptr.condition[k] & ~(1 << n);
                    }
                } else {
                    for (int j = 0; j < nm; j++) {
                        int k = mbr[j];
                        ptr.condition[k] = ptr.condition[k] | 1 << n;
                    }
                }
            } else {
                // not a group
                if (c == '.') {
                    // wild card character so set them all
                    for (int j = 0; j < SETSIZE; j++) {
                        ptr.condition[j] = ptr.condition[j] | 1 << n;
                    }
                } else {
                    // just set the proper bit for this char
                    ptr.condition[c] = ptr.condition[c] | 1 << n;
                }
            }
            n++;
            i++;
        }
        ptr.numconds = n;
    }
}
