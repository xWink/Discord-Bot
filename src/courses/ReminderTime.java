package courses;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the time at the start of {@code !remind 2h call mum} or {@code !remind friday 5pm hand in lab}:
 * a length of time ({@code 10m}, {@code 1h30m}, {@code in 2 hours}), {@code tomorrow}, {@code today}, a weekday, or a date
 * ({@code 2026-12-04}), each optionally followed by a time of day ({@code 5pm}, {@code 17:30}, {@code at noon}). Dates
 * with no time mean 9:00. Times are on the campus clock.
 */
public final class ReminderTime {

    /** The clock the bot talks in. */
    public static final ZoneId ZONE = ZoneId.of("America/Toronto");

    public static final LocalTime DEFAULT_TIME = LocalTime.of(9, 0);
    public static final Duration SOONEST = Duration.ofSeconds(20);
    public static final Duration FURTHEST = Duration.ofDays(366);

    private ReminderTime() { }

    /** What was read: when, and how many of the words it used (the rest is the reminder's text). */
    public static final class Result {
        public final long dueMs;
        public final int used;
        /** Set instead of a time when the words made sense but the time cannot be used. */
        public final String error;

        Result(long dueMs, int used, String error) {
            this.dueMs = dueMs;
            this.used = used;
            this.error = error;
        }
    }

    private static final Pattern PACKED = Pattern.compile("(?i)((\\d+)([smhdw]))+");
    private static final Pattern PART = Pattern.compile("(?i)(\\d+)([smhdw])");
    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final Pattern ISO = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
    private static final Pattern CLOCK = Pattern.compile("(?i)(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?");

    private static long unit(String u, long n) {
        switch (u.toLowerCase(Locale.ROOT)) {
            case "s": return n;
            case "m": return n * 60;
            case "h": return n * 3600;
            case "d": return n * 86400;
            default: return n * 7 * 86400;
        }
    }

    /** @return the unit letter for a word like "hours", or null */
    private static String unitWord(String w) {
        switch (w.toLowerCase(Locale.ROOT)) {
            case "s": case "sec": case "secs": case "second": case "seconds": return "s";
            case "m": case "min": case "mins": case "minute": case "minutes": return "m";
            case "h": case "hr": case "hrs": case "hour": case "hours": return "h";
            case "d": case "day": case "days": return "d";
            case "w": case "wk": case "wks": case "week": case "weeks": return "w";
            default: return null;
        }
    }

    /** @return a time of day at {@code tokens[at]} (and maybe the next word, for "5 pm"), or null; the second element is the words used */
    private static Object[] clock(List<String> tokens, int at) {
        if (at >= tokens.size()) return null;
        String t = tokens.get(at).toLowerCase(Locale.ROOT);
        if (t.equals("noon")) return new Object[]{LocalTime.NOON, 1};
        if (t.equals("midnight")) return new Object[]{LocalTime.MIDNIGHT, 1};
        Matcher m = CLOCK.matcher(t);
        if (!m.matches()) return null;
        int hour = Integer.parseInt(m.group(1));
        int minute = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        String ampm = m.group(3);
        int used = 1;
        if (ampm == null && at + 1 < tokens.size() && tokens.get(at + 1).matches("(?i)am|pm")) {
            ampm = tokens.get(at + 1);
            used = 2;
        }
        if (ampm == null && m.group(2) == null) return null; // a bare number is not a time ("3" in "remind me in 3 days")
        if (minute > 59) return null;
        if (ampm != null) {
            if (hour < 1 || hour > 12) return null;
            hour = hour % 12 + (ampm.equalsIgnoreCase("pm") ? 12 : 0);
        } else if (hour > 23) {
            return null;
        }
        return new Object[]{LocalTime.of(hour, minute), used};
    }

    private static Result at(ZonedDateTime now, ZonedDateTime due, int used) {
        Duration until = Duration.between(now, due);
        if (until.compareTo(SOONEST) < 0) return new Result(0, used, "That time has already passed.");
        if (until.compareTo(FURTHEST) > 0) return new Result(0, used, "I can only remind you up to a year ahead.");
        return new Result(due.toInstant().toEpochMilli(), used, null);
    }

    /**
     * @param tokens the words after the command
     * @param now the current time, on {@link #ZONE}
     * @return the time and how many words it took, or null if the words do not start with a time
     */
    public static Result parse(List<String> tokens, ZonedDateTime now) {
        int i = 0;
        if (i < tokens.size() && tokens.get(i).equalsIgnoreCase("in")) i++;
        if (i >= tokens.size()) return null;

        // a length of time: 10m, 1h30m, 2 hours, 1 day 3 hours
        long seconds = 0;
        int j = i;
        while (j < tokens.size()) {
            String t = tokens.get(j);
            if (PACKED.matcher(t).matches()) {
                Matcher p = PART.matcher(t);
                while (p.find()) seconds += unit(p.group(2), Long.parseLong(p.group(1)));
                j++;
            } else if (NUMBER.matcher(t).matches() && j + 1 < tokens.size() && unitWord(tokens.get(j + 1)) != null) {
                seconds += unit(unitWord(tokens.get(j + 1)), Long.parseLong(t));
                j += 2;
            } else {
                break;
            }
        }
        if (j > i) {
            if (seconds > 400L * 86400) return new Result(0, j, "I can only remind you up to a year ahead.");
            return at(now, now.plusSeconds(seconds), j);
        }

        String first = tokens.get(i).toLowerCase(Locale.ROOT);
        LocalDate date = null;
        int used = i + 1;
        Matcher iso = ISO.matcher(first);
        if (first.equals("tomorrow")) {
            date = now.toLocalDate().plusDays(1);
        } else if (first.equals("today")) {
            date = now.toLocalDate();
        } else if (iso.matches()) {
            try {
                date = LocalDate.of(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(3)));
            } catch (java.time.DateTimeException e) {
                return new Result(0, used, "That is not a real date.");
            }
        } else {
            DayOfWeek day = weekday(first);
            if (day != null) {
                date = now.toLocalDate();
                while (date.getDayOfWeek() != day) date = date.plusDays(1);
            }
        }
        boolean onlyAt = false;
        int k = used;
        if (date == null) {
            // "at 5pm": the next time the clock shows that
            if (!first.equals("at")) return null;
            k = i + 1;
            onlyAt = true;
        } else if (k < tokens.size() && tokens.get(k).equalsIgnoreCase("at")) {
            k++;
        }
        Object[] time = clock(tokens, k);
        if (onlyAt && time == null) return null;
        LocalTime when = time == null ? DEFAULT_TIME : (LocalTime) time[0];
        if (time != null) k += (Integer) time[1];
        else k = used;
        if (onlyAt) {
            ZonedDateTime today = now.toLocalDate().atTime(when).atZone(ZONE);
            return at(now, today.isAfter(now.plus(SOONEST)) ? today : today.plusDays(1), k);
        }
        ZonedDateTime due = date.atTime(when).atZone(ZONE);
        // a bare weekday that is today, with the time already past, means next week
        if (weekday(first) != null && !due.isAfter(now.plus(SOONEST)) && time == null) due = due.plusWeeks(1);
        else if (weekday(first) != null && !due.isAfter(now.plus(SOONEST))) due = due.plusWeeks(1);
        return at(now, due, k);
    }

    private static DayOfWeek weekday(String word) {
        for (DayOfWeek d : DayOfWeek.values()) {
            String full = d.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase(Locale.ROOT);
            if (word.equals(full) || (word.length() >= 3 && full.startsWith(word) && word.length() <= full.length()
                    && (word.length() == 3 || word.equals(full)))) return d;
        }
        return null;
    }
}
