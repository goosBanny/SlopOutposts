package me.goosbanny.outposts.core.schedule;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class CronExpressionTest {

    @Test
    public void testStandardCronMatches() {
        // Run every day at 18:00
        CronExpression cron = CronExpression.parse("0 18 * * *");
        ZonedDateTime matchTime = ZonedDateTime.of(2026, 9, 18, 18, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime nonMatch = ZonedDateTime.of(2026, 9, 18, 18, 1, 0, 0, ZoneOffset.UTC);

        assertTrue(cron.matches(matchTime));
        assertFalse(cron.matches(nonMatch));
    }

    @Test
    public void testIntervalCron() {
        // Every 15 minutes
        CronExpression cron = CronExpression.parse("*/15 * * * *");
        assertTrue(cron.matches(ZonedDateTime.of(2026, 9, 18, 14, 0, 0, 0, ZoneOffset.UTC)));
        assertTrue(cron.matches(ZonedDateTime.of(2026, 9, 18, 14, 15, 0, 0, ZoneOffset.UTC)));
        assertTrue(cron.matches(ZonedDateTime.of(2026, 9, 18, 14, 30, 0, 0, ZoneOffset.UTC)));
        assertTrue(cron.matches(ZonedDateTime.of(2026, 9, 18, 14, 45, 0, 0, ZoneOffset.UTC)));
        assertFalse(cron.matches(ZonedDateTime.of(2026, 9, 18, 14, 10, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    public void testNextMatching() {
        // Next match after 12:05 for "0 13 * * *" should be 13:00 today
        CronExpression cron = CronExpression.parse("0 13 * * *");
        ZonedDateTime current = ZonedDateTime.of(2026, 9, 18, 12, 5, 0, 0, ZoneOffset.UTC);
        ZonedDateTime next = cron.nextMatching(current);

        assertNotNull(next);
        assertEquals(13, next.getHour());
        assertEquals(0, next.getMinute());
        assertEquals(18, next.getDayOfMonth());
    }

    @Test
    public void testDayOfWeekFilter() {
        // Only on Fridays (in standard cron, Fri = 5)
        CronExpression cron = CronExpression.parse("0 20 * * 5");
        // 2026-09-18 is Friday
        ZonedDateTime friday = ZonedDateTime.of(2026, 9, 18, 20, 0, 0, 0, ZoneOffset.UTC);
        // 2026-09-19 is Saturday
        ZonedDateTime saturday = ZonedDateTime.of(2026, 9, 19, 20, 0, 0, 0, ZoneOffset.UTC);

        assertTrue(cron.matches(friday));
        assertFalse(cron.matches(saturday));
    }
}
