package com.pedropacheco.nutrition.backend.userAuth.dto;


import org.springframework.stereotype.Component;

@Component
public class UserAuthMapper {

    public LoginResponseDTO toDTO(String token) {
        return new LoginResponseDTO(token);
    }
}