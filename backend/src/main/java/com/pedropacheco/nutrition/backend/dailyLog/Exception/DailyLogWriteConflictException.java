package com.pedropacheco.nutrition.backend.dailyLog.Exception;

public class DailyLogWriteConflictException extends RuntimeException {
    public DailyLogWriteConflictException(Throwable cause) {
        super("The daily log is being updated by another request. Please try again.", cause);
    }
}
