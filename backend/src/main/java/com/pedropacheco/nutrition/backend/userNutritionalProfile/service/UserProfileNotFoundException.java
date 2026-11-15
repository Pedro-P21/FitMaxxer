package com.pedropacheco.nutrition.backend.userNutritionalProfile.service;

public class UserProfileNotFoundException extends RuntimeException {
    public UserProfileNotFoundException() {
        super("No nutritional profile found. Create your profile first.");
    }
}
