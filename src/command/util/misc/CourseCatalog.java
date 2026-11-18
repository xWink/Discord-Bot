package command.util.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every course in {@code courses.tsv}, read once and indexed. Parses each title line into its parts (code, title,
 * when it is offered, hours, credits) and finds courses by code, by words in the title, or by department.
 */
public final class CourseCatalog {

    /** A course. */
    public static final class Course {
        /** For example {@code CIS*1300}. */
        public final String code;
        public final String department;
        public final int number;
        public final String title;
        /** For example {@code Fall and Winter}, or empty if the catalogue does not say. */
        public final String offered;
        /** For example {@code LEC 3, LAB 2}, or empty. */
        public final String hours;
        public final String credits;
        public final String restrictions;
        public final String prerequisites;
        public final String description;
        final String key;
        final String titleLower;
        final String descriptionLower;

        Course(String code, String department, int number, String title, String offered, String hours, String credits,
               String restrictions, String prerequisites, String description) {
            this.code = code;
            this.department = department;
            this.number = number;
            this.title = title;
            this.offered = offered;
            this.hours = hours;
            this.credits = credits;
            this.restrictions = restrictions;
            this.prerequisites = prerequisites;
            this.description = description;
            this.key = (department + number).toLowerCase(Locale.ROOT);
            this.titleLower = title.toLowerCase(Locale.ROOT);
            this.descriptionLower = description.toLowerCase(Locale.ROOT);
        }

        /** @return 1 to 6: the year the number suggests (1000s are first year) */
        public int level() {
            return number / 1000;
        }

        /** @return the course codes named in the prerequisites, in order, without repeats */
        public List<String> prerequisiteCodes() {
            Set<String> out = new LinkedHashSet<>();
            Matcher m = CODE.matcher(prerequisites);
            while (m.find()) out.add(m.group());
            out.remove(code);
            return new ArrayList<>(out);
        }
    }

    private static final Pattern CODE = Pattern.compile("[A-Z]{2,5}\\*\\d{4}");
    private static final Pattern HEAD = Pattern.compile("^([A-Z]{2,5})\\*(\\d{4})\\s+(.*)$");
    private static final Pattern TAIL = Pattern.compile("\\s*(?:\\(([^)]*)\\)\\s*)?\\[([\\d.]+)\\]\\s*$");
    private static final Pattern OFFERED = Pattern.compile(
            "\\s+((?:(?:Summer|Fall|Winter|Spring)(?:,\\s*and\\s+|,\\s*|\\s+and\\s+)?)+(?:\\s*Only)?|Unspecified)$");

    private static CourseCatalog instance;

    private final Map<String, Course> byKey = new LinkedHashMap<>();
    private final Map<String, List<Course>> byDepartment = new TreeMap<>();

    private CourseCatalog(List<String> lines) {
        for (int i = 1; i < lines.size(); i++) { // the first line is the header
            Course c = parse(lines.get(i));
            if (c == null) continue;
            byKey.putIfAbsent(c.key, c);
            byDepartment.computeIfAbsent(c.department.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(c);
        }
        for (List<Course> list : byDepartment.values()) {
            Collections.sort(list, (a, b) -> Integer.compare(a.number, b.number));
        }
    }

    /** @return the catalogue, read the first time it is asked for */
    public static synchronized CourseCatalog get() {
        if (instance == null) {
            try (InputStream in = CourseCatalog.class.getClassLoader().getResourceAsStream("courses.tsv")) {
                if (in == null) throw new IllegalStateException("courses.tsv is missing");
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) lines.add(line.replace("﻿", ""));
                instance = new CourseCatalog(lines);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return instance;
    }

    /** Builds a catalogue from lines in the same format; for tests. */
    static CourseCatalog of(List<String> lines) {
        return new CourseCatalog(lines);
    }

    static Course parse(String line) {
        String[] cells = line.replace("\"", "").split("\t", -1);
        if (cells.length < 4) return null;
        String head = cells[0].trim().replaceAll("\\s+", " ");
        Matcher tail = TAIL.matcher(head);
        String credits = "";
        String hours = "";
        if (tail.find()) {
            credits = tail.group(2);
            hours = tail.group(1) == null ? "" : tail.group(1).replace(":", "").trim();
            head = head.substring(0, tail.start());
        }
        Matcher h = HEAD.matcher(head);
        if (!h.matches()) return null;
        String rest = h.group(3);
        String offered = "";
        Matcher o = OFFERED.matcher(rest);
        if (o.find()) {
            offered = o.group(1).trim();
            rest = rest.substring(0, o.start());
        }
        return new Course(h.group(1) + "*" + h.group(2), h.group(1), Integer.parseInt(h.group(2)), rest.trim(), offered,
                hours, credits, cells[1].trim(), cells[2].trim(), cells[3].trim());
    }

    public int size() {
        return byKey.size();
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replace("*", "").replaceAll("\\s+", "");
    }

    /** @return the course for a code written any way ({@code cis1300}, {@code CIS*1300}, {@code cis 1300}), or null */
    public Course find(String text) {
        return byKey.get(normalize(text));
    }

    /** @return every course in a department, lowest number first, or an empty list ({@code cis}, {@code CIS}) */
    public List<Course> department(String text) {
        List<Course> list = byDepartment.get(text.trim().toLowerCase(Locale.ROOT));
        return list == null ? Collections.<Course>emptyList() : list;
    }

    /**
     * Finds courses from whatever the player typed: a code or the start of one, or words from the title (every word
     * must appear). Words that only appear in the description rank below title matches. Best matches first.
     */
    public List<Course> search(String text, int limit) {
        String query = text.toLowerCase(Locale.ROOT).replace("*", " ").trim();
        if (query.isEmpty()) return Collections.emptyList();
        String compact = normalize(text);
        String[] words = query.split("\\s+");
        List<Course> exact = new ArrayList<>();
        List<Course> scored = new ArrayList<>();
        Map<Course, Integer> score = new LinkedHashMap<>();
        for (Course c : byKey.values()) {
            int s = 0;
            if (c.key.equals(compact)) s = 1000;
            else if (compact.length() >= 3 && c.key.startsWith(compact)) s = 200 - c.number / 100; // "cis13" -> CIS*13xx
            else {
                boolean inTitle = true;
                boolean inText = true;
                for (String w : words) {
                    if (!c.titleLower.contains(w)) inTitle = false;
                    if (!c.titleLower.contains(w) && !c.descriptionLower.contains(w)) inText = false;
                }
                if (inTitle) s = c.titleLower.equals(query) ? 150 : c.titleLower.startsWith(query) ? 120 : 100;
                else if (inText && words.length > 0 && query.length() >= 4) s = 20;
            }
            if (s > 0) score.put(c, s);
        }
        scored.addAll(score.keySet());
        Collections.sort(scored, (a, b) -> {
            int d = Integer.compare(score.get(b), score.get(a));
            return d != 0 ? d : Integer.compare(a.number, b.number);
        });
        exact.addAll(scored.subList(0, Math.min(limit, scored.size())));
        return exact;
    }

    /** @return the department codes, lower case, for example {@code cis} */
    public Set<String> departments() {
        return byDepartment.keySet();
    }
}
