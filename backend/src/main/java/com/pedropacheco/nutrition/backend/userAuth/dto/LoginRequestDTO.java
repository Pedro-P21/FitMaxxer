package com.pedropacheco.nutrition.backend.userAuth.dto;

public class LoginRequestDTO {
    public final String identifier;
    public final String password;

    public LoginRequestDTO(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }


    public String getIdentifier() { return identifier; }
    public String getPassword() { return password; }
}