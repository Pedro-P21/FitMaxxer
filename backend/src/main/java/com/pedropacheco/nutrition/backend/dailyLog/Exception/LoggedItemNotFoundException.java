package com.pedropacheco.nutrition.backend.dailyLog.Exception;

public class LoggedItemNotFoundException extends RuntimeException {
    public LoggedItemNotFoundException() {
        super("Logged item was not found in today's log.");
    }
}
