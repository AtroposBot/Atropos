package dev.laarryy.atropos.utils;

public final class TimestampMaker {

    public enum TimestampType {
        SHORT_TIME,
        LONG_TYPE,
        SHORT_DATE,
        LONG_DATE,
        SHORT_DATETIME,
        LONG_DATETIME,
        RELATIVE
    }

    private TimestampMaker() {}

    public static String getTimestampFromEpochSecond(long epochSecond, TimestampType timestampType) {

        return getTypeLetter(timestampType, epochSecond);
    }

    private static String getTypeLetter(TimestampType timestampType, long newMilli) {
        String typeLetter = switch (timestampType) {
            case SHORT_TIME -> "t";
            case LONG_TYPE -> "T";
            case SHORT_DATE -> "d";
            case LONG_DATE -> "D";
            case SHORT_DATETIME -> "f";
            case LONG_DATETIME -> "F";
            case RELATIVE -> "R";
        };

        return "<t:" + newMilli + ":" + typeLetter + ">";
    }
}
