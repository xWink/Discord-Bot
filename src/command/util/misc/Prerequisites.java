package command.util.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a calendar prerequisite line such as {@code "CIS*1300 or CIS*1500, MATH*1200"} and checks it against the
 * courses someone has finished. A comma or "and" means both, "or" means either and binds tighter than a comma, and
 * brackets group. Anything that is not a course code ("4.00 credits", "permission of the instructor") cannot be
 * checked, so a line that depends on it comes out as {@link Status#UNKNOWN} rather than yes or no.
 */
public final class Prerequisites {

    /** The answer for a requirement. */
    public enum Status { MET, UNMET, UNKNOWN }

    /** One piece of a requirement. */
    public abstract static class Node {
        abstract Status status(Set<String> done);

        /** @return what is still needed, or an empty string if nothing is */
        abstract String missing(Set<String> done);
    }

    private static final class Code extends Node {
        final String code;

        Code(String code) {
            this.code = code;
        }

        @Override
        Status status(Set<String> done) {
            return done.contains(code) ? Status.MET : Status.UNMET;
        }

        @Override
        String missing(Set<String> done) {
            return done.contains(code) ? "" : code;
        }
    }

    private static final class Other extends Node {
        final String text;

        Other(String text) {
            this.text = text;
        }

        @Override
        Status status(Set<String> done) {
            return Status.UNKNOWN;
        }

        @Override
        String missing(Set<String> done) {
            return text + " (I cannot check this one)";
        }
    }

    /** Some number of its parts must be met: all of them, any one of them, or "N of" them. */
    private static final class Group extends Node {
        /** 0 means every part. */
        final int need;
        final List<Node> parts = new ArrayList<>();

        Group(int need) {
            this.need = need;
        }

        private int needed() {
            return need == 0 ? parts.size() : Math.min(need, parts.size());
        }

        @Override
        Status status(Set<String> done) {
            int met = 0;
            int unknown = 0;
            for (Node n : parts) {
                Status s = n.status(done);
                if (s == Status.MET) met++;
                else if (s == Status.UNKNOWN) unknown++;
            }
            if (met >= needed()) return Status.MET;
            return met + unknown >= needed() ? Status.UNKNOWN : Status.UNMET;
        }

        @Override
        String missing(Set<String> done) {
            List<String> out = new ArrayList<>();
            for (Node n : parts) {
                if (n.status(done) == Status.MET) continue;
                String m = n.missing(done);
                if (!m.isEmpty()) out.add(m);
            }
            if (out.isEmpty()) return "";
            if (needed() == parts.size()) return String.join(", ", out);
            int stillNeeded = needed() - (parts.size() - out.size());
            String what = String.join(" or ", out);
            return out.size() == 1 ? what : (stillNeeded == 1 ? "one of: " : stillNeeded + " of: ") + what;
        }
    }

    private final Node root;
    private final boolean empty;

    private Prerequisites(Node root, boolean empty) {
        this.root = root;
        this.empty = empty;
    }

    // ---------------------------------------------------------------- reading

    private static final Pattern CODE = Pattern.compile("[A-Z]{2,5}\\*\\d{4}");
    private static final Pattern OP = Pattern.compile("(?i)(or|and)(?=[\\s(]|$)");

    private static final Pattern COUNT = Pattern.compile("(?i)(\\d+|one|two|three|four) of");

    private static int count(String word) {
        switch (word.toLowerCase(Locale.ROOT)) {
            case "one": return 1;
            case "two": return 2;
            case "three": return 3;
            case "four": return 4;
            default: return Integer.parseInt(word);
        }
    }

    private enum Kind { CODE, OPEN, CLOSE, COMMA, OR, AND, OTHER }

    private static final class Token {
        final Kind kind;
        final String text;

        Token(Kind kind, String text) {
            this.kind = kind;
            this.text = text;
        }
    }

    private static List<Token> tokenize(String text) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || c == '.') {
                i++;
                continue;
            }
            Matcher code = CODE.matcher(text).region(i, n);
            if (code.lookingAt()) {
                out.add(new Token(Kind.CODE, code.group()));
                i = code.end();
            } else if (c == '(') {
                out.add(new Token(Kind.OPEN, "("));
                i++;
            } else if (c == ')') {
                out.add(new Token(Kind.CLOSE, ")"));
                i++;
            } else if (c == ',' || c == ';') {
                out.add(new Token(Kind.COMMA, ","));
                i++;
            } else {
                Matcher op = OP.matcher(text).region(i, n);
                if (op.lookingAt()) {
                    out.add(new Token(op.group().equalsIgnoreCase("or") ? Kind.OR : Kind.AND, op.group()));
                    i = op.end();
                    continue;
                }
                // free text, up to the next code, bracket, comma, or the word or / and
                int j = i;
                while (j < n) {
                    char d = text.charAt(j);
                    if (d == '(' || d == ')' || d == ',' || d == ';') break;
                    if (CODE.matcher(text).region(j, n).lookingAt()) break;
                    if ((j == i || Character.isWhitespace(text.charAt(j - 1))) && j > i && OP.matcher(text).region(j, n).lookingAt()) break;
                    j++;
                }
                String chunk = text.substring(i, j).trim().replaceAll("[\\s.]+$", "");
                if (!chunk.isEmpty()) out.add(new Token(Kind.OTHER, chunk));
                i = Math.max(j, i + 1);
            }
        }
        return out;
    }

    private static final class Parser {
        final List<Token> tokens;
        int at;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        boolean more() {
            return at < tokens.size();
        }

        Kind peek() {
            return at < tokens.size() ? tokens.get(at).kind : null;
        }

        /** Both sides: terms joined by a comma, "and" or just by sitting next to each other. */
        Node all() {
            Group g = new Group(0);
            g.parts.add(either());
            while (more() && peek() != Kind.CLOSE) {
                if (peek() == Kind.COMMA || peek() == Kind.AND) at++;
                if (!more() || peek() == Kind.CLOSE) break;
                g.parts.add(either());
            }
            return g.parts.size() == 1 ? g.parts.get(0) : g;
        }

        Node either() {
            Group g = new Group(1);
            g.parts.add(term());
            while (peek() == Kind.OR) {
                at++;
                g.parts.add(term());
            }
            return g.parts.size() == 1 ? g.parts.get(0) : g;
        }

        Node term() {
            if (!more()) return new Other("");
            Token t = tokens.get(at);
            switch (t.kind) {
                case CODE:
                    at++;
                    return new Code(t.text);
                case OTHER:
                    at++;
                    return new Other(t.text);
                case OPEN: {
                    at++;
                    // "(1 of A, B, C)" means any one (or two...) of the list, not all of it
                    if (peek() == Kind.OTHER) {
                        Matcher m = COUNT.matcher(tokens.get(at).text);
                        if (m.matches()) {
                            at++;
                            Group some = new Group(count(m.group(1)));
                            Node list = all();
                            if (list instanceof Group && ((Group) list).need == 0) some.parts.addAll(((Group) list).parts);
                            else some.parts.add(list);
                            if (peek() == Kind.CLOSE) at++;
                            return some;
                        }
                    }
                    Node inner = all();
                    if (peek() == Kind.CLOSE) at++;
                    return inner;
                }
                default: // a stray operator or bracket: skip it
                    at++;
                    return more() ? term() : new Other("");
            }
        }
    }

    /**
     * @param text the prerequisite text from the calendar, with or without its "Prerequisite(s): " label
     * @return the requirement; for "NONE" or blank text it is always met
     */
    public static Prerequisites parse(String text) {
        return parse(text, "");
    }

    /** Advice after the requirement ("Although not required, ...") is not part of it. */
    private static final Pattern ADVICE = Pattern.compile("(?i)[.;,]?\\s*(although|it is (strongly )?recommended|recommended|"
            + "it is advisable|students are advised).*$");

    /** @return the requirement of a course; its own code, if the text mentions it, is ignored */
    public static Prerequisites of(CourseCatalog.Course course) {
        return parse(course.prerequisites, course.code);
    }

    /**
     * @param ownCode the course the line belongs to; mentions of it (as in "recommended before MCS*2600") are ignored
     */
    public static Prerequisites parse(String text, String ownCode) {
        String t = text == null ? "" : text.replace("Prerequisite(s):", "").trim();
        t = ADVICE.matcher(t).replaceFirst("").trim();
        if (t.isEmpty() || t.equalsIgnoreCase("none")) return new Prerequisites(new Group(0), true);
        List<Token> tokens = new ArrayList<>();
        for (Token tok : tokenize(t)) if (!(tok.kind == Kind.CODE && tok.text.equals(ownCode))) tokens.add(tok);
        if (tokens.isEmpty()) return new Prerequisites(new Group(0), true);
        return new Prerequisites(new Parser(tokens).all(), false);
    }

    // ---------------------------------------------------------------- asking

    /** @return true if the course has no prerequisite at all */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * @param done the codes of finished courses (include courses that count as the same, see {@link #closure})
     * @return {@link Status#MET} only if everything the line asks can be shown to be done
     */
    public Status check(Set<String> done) {
        return empty ? Status.MET : root.status(done);
    }

    /** @return what is still needed, in words, or an empty string if nothing is */
    public String missing(Set<String> done) {
        return empty ? "" : root.missing(done);
    }

    /**
     * Adds the courses that count as the same one: if a student finished BUS*3230 and ACCT*3230 counts as the same,
     * both are in the set.
     */
    public static Set<String> closure(CourseCatalog catalog, Set<String> finished) {
        Set<String> all = new java.util.LinkedHashSet<>(finished);
        for (String code : finished) {
            CourseCatalog.Course c = catalog.find(code);
            if (c != null) all.addAll(c.equateCodes());
        }
        // equivalence runs both ways: a course that names a finished course as its equal also counts
        for (String dept : catalog.departments()) {
            for (CourseCatalog.Course c : catalog.department(dept)) {
                for (String eq : c.equateCodes()) {
                    if (finished.contains(eq)) all.add(c.code);
                }
            }
        }
        return all;
    }

    @Override
    public String toString() {
        return empty ? "none" : root.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }
}
