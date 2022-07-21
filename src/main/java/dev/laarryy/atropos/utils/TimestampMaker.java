package dev.laarryy.atropos.utils;

public final class TimestampMaker {

    public enum TimestampType {
        SHORT_TIME,
        LONG_TIME,
        SHORT_DATE,
        LONG_DATE,
        SHORT_DATETIME,
        LONG_DATETIME,
        RELATIVE
    }

    private TimestampMaker() {}

    /**
     *
     * @param epochSecond Epoch second time point
     * @param timestampType Which type of Discord-style timestamp is needed
     * @return a {@link String} representing a formatted Discord-style timestamp for the provided epochSecond
     */

    public static String getTimestampFromEpochSecond(long epochSecond, TimestampType timestampType) {

        return getTypeLetter(timestampType, epochSecond);
    }

    private static String getTypeLetter(TimestampType timestampType, long epochSecond) {
        String typeLetter = switch (timestampType) {
            case SHORT_TIME -> "t";
            case LONG_TIME -> "T";
            case SHORT_DATE -> "d";
            case LONG_DATE -> "D";
            case SHORT_DATETIME -> "f";
            case LONG_DATETIME -> "F";
            case RELATIVE -> "R";
        };

        return "<t:" + epochSecond + ":" + typeLetter + ">";
    }
}
