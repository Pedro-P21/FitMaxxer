package com.pedropacheco.nutrition.backend.userAuth.dto;

public class RegisterRequestDTO {
    public final String email;
    public final String password;
    public final String userId;

    public RegisterRequestDTO(String email, String password, String userId) {
        this.email = email;
        this.password = password;
        this.userId = userId;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getUserId() { return userId; }
}