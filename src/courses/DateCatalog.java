package courses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The important dates from the academic calendar ({@code dates.tsv}, made by {@code res/scrape.py}). */
public final class DateCatalog {

    /** One thing that happens on one day. */
    public static final class Event {
        public final String term;
        public final LocalDate date;
        public final String description;

        Event(String term, LocalDate date, String description) {
            this.term = term;
            this.date = date;
            this.description = description;
        }

        /** @return true for the veterinary programme's own calendar, which most people do not want */
        public boolean veterinary() {
            return term.startsWith("D.V.M.");
        }
    }

    private static DateCatalog instance;

    private final List<Event> events = new ArrayList<>();

    private DateCatalog(List<String> lines) {
        for (int i = 1; i < lines.size(); i++) {
            String[] cells = lines.get(i).split("\t", -1);
            if (cells.length < 3) continue;
            try {
                events.add(new Event(cells[0].trim(), LocalDate.parse(cells[1].trim()), cells[2].trim()));
            } catch (java.time.format.DateTimeParseException e) {
                // a line that is not a date is skipped
            }
        }
        events.sort((a, b) -> a.date.compareTo(b.date));
    }

    public static synchronized DateCatalog get() {
        if (instance == null) {
            try (InputStream in = DateCatalog.class.getClassLoader().getResourceAsStream("dates.tsv")) {
                List<String> lines = new ArrayList<>();
                if (in != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    String line;
                    while ((line = r.readLine()) != null) lines.add(line.replace("﻿", ""));
                }
                instance = new DateCatalog(lines);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return instance;
    }

    static DateCatalog of(List<String> lines) {
        return new DateCatalog(lines);
    }

    public int size() {
        return events.size();
    }

    /**
     * @param from the first day to include
     * @param keywords every word must appear in the description or term (empty for all)
     * @param withVeterinary whether to include the veterinary programme's dates
     * @return up to {@code limit} events on or after {@code from}, soonest first
     */
    public List<Event> upcoming(LocalDate from, List<String> keywords, boolean withVeterinary, int limit) {
        List<Event> out = new ArrayList<>();
        for (Event e : events) {
            if (e.date.isBefore(from) || (e.veterinary() && !withVeterinary)) continue;
            String hay = (e.description + " " + e.term).toLowerCase(Locale.ROOT);
            boolean all = true;
            for (String k : keywords) if (!hay.contains(k.toLowerCase(Locale.ROOT))) all = false;
            if (!all) continue;
            out.add(e);
            if (out.size() == limit) break;
        }
        return out;
    }

    /** @return the events on one day, in file order (the number a reminder button uses, counting from 1) */
    public List<Event> on(LocalDate day, boolean withVeterinary) {
        List<Event> out = new ArrayList<>();
        for (Event e : events) if (e.date.equals(day) && (withVeterinary || !e.veterinary())) out.add(e);
        return out;
    }
}
