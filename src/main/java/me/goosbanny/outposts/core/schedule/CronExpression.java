package me.goosbanny.outposts.core.schedule;

import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;
import java.util.Map;

/**
 * Lightweight, zero-dependency 5-part cron expression evaluator.
 * Format: minute (0-59) hour (0-23) dayOfMonth (1-31) month (1-12) dayOfWeek (0-7, 0 or 7 = Sunday).
 */
public class CronExpression {

    private final String expression;
    private final BitSet minutes = new BitSet(60);
    private final BitSet hours = new BitSet(24);
    private final BitSet daysOfMonth = new BitSet(32);
    private final BitSet months = new BitSet(13);
    private final BitSet daysOfWeek = new BitSet(8);

    public CronExpression(@NotNull String expression) {
        this.expression = expression.trim();
        parseInternal(this.expression);
    }

    public static CronExpression parse(@NotNull String expression) {
        return new CronExpression(expression);
    }

    private void parseInternal(String expr) {
        String[] parts = expr.split("\\s+");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid cron expression (must have 5 parts): '" + expr + "'");
        }

        parseField(parts[0], minutes, 0, 59, Map.of());
        parseField(parts[1], hours, 0, 23, Map.of());
        parseField(parts[2], daysOfMonth, 1, 31, Map.of());

        Map<String, Integer> monthNames = Map.ofEntries(
                Map.entry("JAN", 1), Map.entry("FEB", 2), Map.entry("MAR", 3), Map.entry("APR", 4),
                Map.entry("MAY", 5), Map.entry("JUN", 6), Map.entry("JUL", 7), Map.entry("AUG", 8),
                Map.entry("SEP", 9), Map.entry("OCT", 10), Map.entry("NOV", 11), Map.entry("DEC", 12)
        );
        parseField(parts[3], months, 1, 12, monthNames);

        Map<String, Integer> dayNames = Map.ofEntries(
                Map.entry("SUN", 0), Map.entry("MON", 1), Map.entry("TUE", 2), Map.entry("WED", 3),
                Map.entry("THU", 4), Map.entry("FRI", 5), Map.entry("SAT", 6)
        );
        parseField(parts[4], daysOfWeek, 0, 7, dayNames);
        // Normalize day 7 (Sunday) to day 0
        if (daysOfWeek.get(7)) {
            daysOfWeek.set(0);
        }
    }

    private void parseField(String part, BitSet bits, int min, int max, Map<String, Integer> aliases) {
        String upper = part.toUpperCase();
        for (Map.Entry<String, Integer> entry : aliases.entrySet()) {
            upper = upper.replace(entry.getKey(), String.valueOf(entry.getValue()));
        }

        String[] subParts = upper.split(",");
        for (String sub : subParts) {
            if (sub.equals("*")) {
                bits.set(min, max + 1);
            } else if (sub.startsWith("*/")) {
                int step = Integer.parseInt(sub.substring(2));
                for (int i = min; i <= max; i += step) {
                    bits.set(i);
                }
            } else if (sub.contains("-")) {
                String[] range = sub.split("-");
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                bits.set(start, end + 1);
            } else {
                int val = Integer.parseInt(sub);
                bits.set(val);
            }
        }
    }

    public boolean matches(@NotNull ZonedDateTime zdt) {
        int minute = zdt.getMinute();
        int hour = zdt.getHour();
        int day = zdt.getDayOfMonth();
        int month = zdt.getMonthValue();
        int dow = zdt.getDayOfWeek().getValue() % 7; // Java DayOfWeek: Mon=1..Sun=7, modulo 7 -> Sun=0, Mon=1

        return minutes.get(minute)
                && hours.get(hour)
                && daysOfMonth.get(day)
                && months.get(month)
                && (daysOfWeek.get(dow) || (dow == 0 && daysOfWeek.get(7)));
    }

    /**
     * Calculates the next matching ZonedDateTime at or after the given time (truncated to whole minutes).
     */
    @NotNull
    public ZonedDateTime nextMatching(@NotNull ZonedDateTime from) {
        ZonedDateTime current = from.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        // Search up to 366 days ahead
        ZonedDateTime limit = current.plusDays(366);

        while (current.isBefore(limit)) {
            if (!months.get(current.getMonthValue())) {
                current = current.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0);
                continue;
            }

            int dow = current.getDayOfWeek().getValue() % 7;
            boolean dowMatch = daysOfWeek.get(dow) || (dow == 0 && daysOfWeek.get(7));
            boolean domMatch = daysOfMonth.get(current.getDayOfMonth());

            if (!domMatch || !dowMatch) {
                current = current.plusDays(1).withHour(0).withMinute(0);
                continue;
            }

            if (!hours.get(current.getHour())) {
                current = current.plusHours(1).withMinute(0);
                continue;
            }

            if (!minutes.get(current.getMinute())) {
                current = current.plusMinutes(1);
                continue;
            }

            return current;
        }

        return from.plusHours(1);
    }

    public String getExpression() {
        return expression;
    }
}
