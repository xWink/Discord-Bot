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
 * Every course in {@code courses.tsv} (made by {@code res/scrape.py} from the undergraduate and graduate calendars),
 * read once and indexed. Finds courses by code, by words in the title or description, or by department.
 */
public final class CourseCatalog {

    /** A course. */
    public static final class Course {
        /** For example {@code CIS*1300}. */
        public final String code;
        /** The subject code, for example {@code CIS}. */
        public final String department;
        public final int number;
        public final String title;
        /** {@code undergraduate} or {@code graduate}: which calendar the course comes from. */
        public final String source;
        /** For example {@code Fall and Winter}, or empty if the catalogue does not say. */
        public final String offered;
        /** For example {@code LEC 3, LAB 2}, or empty. */
        public final String hours;
        public final String credits;
        public final String restrictions;
        public final String prerequisites;
        public final String corequisites;
        /** Courses that count as the same course, for example {@code BUS*3230}. */
        public final String equates;
        /** A note on how it is offered, for example that it is also available by distance education. */
        public final String offering;
        /** The department or school that runs it, for example {@code School of Computer Science}. */
        public final String unit;
        public final String locations;
        public final String description;
        final String key;
        final String titleLower;
        final String descriptionLower;

        Course(Map<String, String> row, String code, String department, int number) {
            this.code = code;
            this.department = department;
            this.number = number;
            this.title = row.get("title");
            this.source = row.get("source");
            this.offered = row.get("offered");
            this.hours = row.get("hours");
            this.credits = row.get("credits");
            this.restrictions = row.get("restrictions");
            this.prerequisites = row.get("prerequisites");
            this.corequisites = row.get("corequisites");
            this.equates = row.get("equates");
            this.offering = row.get("offering");
            this.unit = row.get("departments");
            this.locations = row.get("locations");
            this.description = row.get("description");
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
            return codesIn(prerequisites);
        }

        /** @return the course codes that must be taken at the same time */
        public List<String> corequisiteCodes() {
            return codesIn(corequisites);
        }

        /** @return the course codes of the courses that count as this one */
        public List<String> equateCodes() {
            return codesIn(equates);
        }

        private List<String> codesIn(String text) {
            Set<String> out = new LinkedHashSet<>();
            Matcher m = CODE.matcher(text);
            while (m.find()) out.add(m.group());
            out.remove(code);
            return new ArrayList<>(out);
        }
    }

    private static final Pattern CODE = Pattern.compile("[A-Z]{2,5}\\*\\d{4}");
    private static final Pattern WHOLE_CODE = Pattern.compile("^([A-Z]{2,5})\\*(\\d{4})$");
    private static CourseCatalog instance;

    private final Map<String, Course> byKey = new LinkedHashMap<>();
    private final Map<String, List<Course>> byDepartment = new TreeMap<>();

    /** The columns every course has; a column missing from the file reads as empty. */
    private static final String[] COLUMNS = {"source", "code", "title", "offered", "hours", "credits", "description",
            "prerequisites", "corequisites", "equates", "restrictions", "offering", "departments", "locations"};

    private CourseCatalog(List<String> lines) {
        if (lines.isEmpty()) return;
        // columns are found by the names in the first line, so the file can gain columns without breaking this
        String[] header = lines.get(0).split("\t", -1);
        Map<String, Integer> at = new LinkedHashMap<>();
        for (int i = 0; i < header.length; i++) at.put(header[i].trim().toLowerCase(Locale.ROOT), i);
        for (int i = 1; i < lines.size(); i++) {
            Course c = parse(lines.get(i), at);
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

    /** Builds a catalogue from lines in the same format (the header line first); for tests. */
    static CourseCatalog of(List<String> lines) {
        return new CourseCatalog(lines);
    }

    static Course parse(String line, Map<String, Integer> at) {
        String[] cells = line.split("\t", -1);
        Map<String, String> row = new LinkedHashMap<>();
        for (String column : COLUMNS) {
            Integer i = at.get(column);
            row.put(column, i == null || i >= cells.length ? "" : cells[i].trim());
        }
        Matcher m = WHOLE_CODE.matcher(row.get("code"));
        if (!m.matches() || row.get("title").isEmpty()) return null;
        return new Course(row, row.get("code"), m.group(1), Integer.parseInt(m.group(2)));
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
     * Finds courses from whatever the player typed: a code or the start of one, a bare course number
     * ({@code 4820} finds the 4820 course of every department), or words from the title (every word must appear).
     * Words that only appear in the description rank below title matches. Best matches first.
     */
    public List<Course> search(String text, int limit) {
        return search(text, limit, true);
    }

    /**
     * Like {@link #search(String, int)} but only courses whose code or title matches, never ones that merely mention
     * the words. Use this when a command has to pick a course, so a stray mention cannot be mistaken for the one meant.
     */
    public List<Course> match(String text, int limit) {
        return search(text, limit, false);
    }

    private List<Course> search(String text, int limit, boolean includeDescriptions) {
        String query = text.toLowerCase(Locale.ROOT).replace("*", " ").trim();
        if (query.isEmpty()) return Collections.emptyList();
        String compact = normalize(text);
        boolean number = compact.matches("\\d{4}");
        String[] words = query.split("\\s+");
        Map<Course, Integer> score = new LinkedHashMap<>();
        for (Course c : byKey.values()) {
            int s = 0;
            if (number) {
                if (String.valueOf(c.number).equals(compact)) s = 150;
            } else if (c.key.equals(compact)) {
                s = 1000;
            } else if (compact.length() >= 3 && c.key.startsWith(compact)) {
                s = 200 - c.number / 100; // "cis13" -> CIS*13xx
            } else {
                boolean inTitle = true;
                boolean inText = true;
                for (String w : words) {
                    if (!c.titleLower.contains(w)) inTitle = false;
                    if (!c.titleLower.contains(w) && !c.descriptionLower.contains(w)) inText = false;
                }
                if (inTitle) s = c.titleLower.equals(query) ? 150 : c.titleLower.startsWith(query) ? 120 : 100;
                else if (includeDescriptions && inText && query.length() >= 4) s = 20;
            }
            if (s > 0) score.put(c, s);
        }
        List<Course> scored = new ArrayList<>(score.keySet());
        Collections.sort(scored, (a, b) -> {
            int d = Integer.compare(score.get(b), score.get(a));
            if (d != 0) return d;
            d = Integer.compare(a.number, b.number);
            return d != 0 ? d : a.code.compareTo(b.code);
        });
        return new ArrayList<>(scored.subList(0, Math.min(limit, scored.size())));
    }

    /** @return the department codes, lower case, for example {@code cis} */
    public Set<String> departments() {
        return byDepartment.keySet();
    }
}
