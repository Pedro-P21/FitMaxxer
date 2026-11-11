package com.pedropacheco.nutrition.backend.dailyLog.service;

public class NutritionalProfileRequiredException extends RuntimeException {
    public NutritionalProfileRequiredException() {
        super("To use the daily log feature, you must have a nutritional profile.");
    }
}
