package dev.laarryy.Eris.managers;

import dev.laarryy.Eris.listeners.logging.LoggingListener;

public class LoggingListenerManager {
    private static LoggingListenerManager instance;
    private final LoggingListener loggingListener;

    public LoggingListenerManager(LoggingListener loggingListener) {
        this.loggingListener = loggingListener;
    }

    public static LoggingListenerManager getManager() {
        if (instance == null) {
            instance = new LoggingListenerManager(new LoggingListener());
        }
        return instance;
    }

    public LoggingListener getLoggingListener() {
        return loggingListener;
    }
}
