package com.pedropacheco.nutrition.backend.userAuth.dto;

public class LoginResponseDTO {
    public final String token;
    private String type = "Bearer";

    public LoginResponseDTO(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
    public String getType() {
        return type;
    }

}
