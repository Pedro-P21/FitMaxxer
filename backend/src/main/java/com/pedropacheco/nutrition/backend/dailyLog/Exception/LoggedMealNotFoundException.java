package com.pedropacheco.nutrition.backend.dailyLog.Exception;

public class LoggedMealNotFoundException extends RuntimeException {
    public LoggedMealNotFoundException() {
        super("The selected historical meal was not found.");
    }
}
