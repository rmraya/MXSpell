package com.maxprograms.mxspell;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class AffixParser {

    public static final int SETSIZE = 2048; // 1024
    public static final int MAXLNLEN = SETSIZE;

    Map<String, List<Prefix>> prefix;
    Map<String, List<Suffix>> suffix;

    char[] tryCharacter;
    int[] mbr = new int[MAXLNLEN];
    private String filename;

    private String line;
    private int lineNr;
    private StringTokenizer tokenizer;
    String encoding;
    String compoundFlag;
    int compoundMinimalChars = -1;
    Map<String, String> replacement;

    private static final int TAG_TRY = 1;
    private static final int TAG_SET = 2;
    private static final int TAG_COMPOUNDFLAG = 3;
    private static final int TAG_COMPOUNDMIN = 4;
    private static final int TAG_REP = 5;
    private static final int TAG_PFX = 6;
    private static final int TAG_SFX = 7;

    AffixParser(String filename) throws Exception {
        this.filename = filename;
        String originalEncoding = "";
        try (FileReader reader = new FileReader(filename)) {
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.trim().startsWith("SET ")) {
                        originalEncoding = checkEncoding(line.trim().substring(4).trim());
                        break;
                    }
                }
                if (originalEncoding == null) {
                    MessageFormat mf = new MessageFormat("Unsupported dictionary encoding: {0}");
                    Object[] args = { line.trim().substring(4).trim() };
                    throw new Exception(mf.format(args));
                }
                parseAffixFile(bufferedReader);
            }
        }
    }

    private String checkEncoding(String string) {
        Map<String, Charset> charsets = new TreeMap<>(Charset.availableCharsets());
        Set<String> keys = charsets.keySet();
        String[] codes = new String[keys.size()];
        Iterator<String> i = keys.iterator();
        int j = 0;
        while (i.hasNext()) {
            Charset cset = charsets.get(i.next());
            if (cset.displayName().equalsIgnoreCase(string)) {
                return cset.displayName();
            }
            codes[j++] = cset.displayName();
        }
        if (string.matches(".*\\d\\d\\d.*")) {
            // microsoft or ISO based codepage
            StringBuffer builder = new StringBuffer();
            for (j = 0; !Character.isDigit(string.charAt(j)); j++) {
                // skip non-numeric characteres
            }
            for (; j < string.length(); j++) {
                builder.append(string.charAt(j));
            }
            string = builder.toString();
            for (j = 0; j < codes.length; j++) {
                if (codes[j].indexOf(string) != -1) {
                    return codes[j];
                }
            }
        }
        return null;
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

    void parseAffixFile(BufferedReader bufferedReader) throws IOException, IOException {
        suffix = new HashMap<>();
        prefix = new HashMap<>();
        Map<String, Integer> tagmap = new HashMap<>();
        tagmap.put("TRY", TAG_TRY);
        tagmap.put("SET", TAG_SET);
        tagmap.put("COMPOUNDFLAG", TAG_COMPOUNDFLAG);
        tagmap.put("COMPOUNDMIN", TAG_COMPOUNDMIN);
        tagmap.put("REP", TAG_REP);
        tagmap.put("PFX", TAG_PFX);
        tagmap.put("SFX", TAG_SFX);
        line = bufferedReader.readLine();
        while (line != null) {
            lineNr++;
            tokenizer = new StringTokenizer(line.trim());
            if (tokenizer.hasMoreTokens()) {
                Integer tag = tagmap.get(tokenizer.nextToken());
                if (tag != null) {
                    switch (tag.intValue()) {
                        case TAG_TRY:
                            if (tryCharacter != null) {
                                MessageFormat mf = new MessageFormat("{0}:{1} : duplicate TRY strings: {2}");
                                Object[] args = { filename, "" + lineNr, line };
                                throw new IOException(mf.format(args));
                            }
                            tryCharacter = parseSingleValueLine("TRY").toCharArray();
                            break;
                        case TAG_SET:
                            if (encoding != null) {
                                MessageFormat mf = new MessageFormat("{0}:{1} : duplicate SET strings: {2}");
                                Object[] args = { filename, "" + lineNr, line };
                                throw new IOException(mf.format(args));
                            }
                            encoding = checkEncoding(parseSingleValueLine("SET"));
                            break;
                        case TAG_COMPOUNDFLAG:
                            if (compoundFlag != null) {
                                MessageFormat mf = new MessageFormat("{0}:{1} : duplicate compound flags: {2}");
                                Object[] args = { filename, "" + lineNr, line };
                                throw new IOException(mf.format(args));
                            }
                            compoundFlag = parseSingleValueLine("COMPOUNDFLAG");
                            break;
                        case TAG_COMPOUNDMIN:
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
                        case TAG_REP:
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
                        case TAG_SFX:
                            parseAffix(bufferedReader, "SFX", suffix);
                            break;
                        case TAG_PFX:
                            parseAffix(bufferedReader, "PFX", prefix);
                            break;
                        default:
                            MessageFormat mf = new MessageFormat("{0}:{1} : unknown tag: {2}");
                            Object[] args = { filename, "" + lineNr, line };
                            throw new IOException(mf.format(args));
                    }
                    // ignore unknown tags
                    /*
                     * } else {
                     * throw new IOException(filename + ':' + lineNr + ": unknown tag: " + line);
                     */
                }
            }
            line = bufferedReader.readLine();
        }
    }

    @SuppressWarnings("unchecked")
    private void parseAffix(BufferedReader bufferedReader, String tagname, Map map) throws IOException, IOException {
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
                if (map == suffix) {
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
                    if (map == suffix) {
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

    public String getEncoding() {
        return encoding;
    }
}
