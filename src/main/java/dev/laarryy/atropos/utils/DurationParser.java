package dev.laarryy.atropos.utils;

import com.google.common.collect.ImmutableMap;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class DurationParser {
    private DurationParser() {
    }

    private static final Map<ChronoUnit, String> UNITS_PATTERNS = ImmutableMap.of(
            ChronoUnit.YEARS, "y(?:ear)?s?",
            ChronoUnit.MONTHS, "mo(?:nth)?s?",
            ChronoUnit.WEEKS, "w(?:eek)?s?",
            ChronoUnit.DAYS, "d(?:ay)?s?",
            ChronoUnit.HOURS, "h(?:our|r)?s?",
            ChronoUnit.MINUTES, "m(?:inute|in)?s?",
            ChronoUnit.SECONDS, "s(?:econd|ec)?s?"
    );

    private static final ChronoUnit[] UNITS = UNITS_PATTERNS.keySet().toArray(new ChronoUnit[0]);

    private static final String PATTERN_STRING = UNITS_PATTERNS.values().stream()
            .map(pattern -> "(?:(\\d+)\\s*" + pattern + "[,\\s]*)?")
            .collect(Collectors.joining("", "^\\s*", "$"));

    private static final Pattern PATTERN = Pattern.compile(PATTERN_STRING, Pattern.CASE_INSENSITIVE);

    public static Duration parseDuration(String input) throws IllegalArgumentException {
        Matcher matcher = PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("unable to parse duration: " + input);
        }

        Duration duration = Duration.ZERO;
        for (int i = 0; i < UNITS.length; i++) {
            ChronoUnit unit = UNITS[i];
            int g = i + 1;

            if (matcher.group(g) != null && !matcher.group(g).isEmpty()) {
                int n = Integer.parseInt(matcher.group(g));
                if (n > 0) {
                    duration = duration.plus(unit.getDuration().multipliedBy(n));
                }
            }
        }

        return duration;
    }

}
