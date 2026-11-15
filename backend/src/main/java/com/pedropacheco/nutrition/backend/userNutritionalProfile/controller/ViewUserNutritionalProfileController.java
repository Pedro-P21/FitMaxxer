package com.pedropacheco.nutrition.backend.userNutritionalProfile.controller;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.UserProfileResponseDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.ViewUserNutritionalProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
public class ViewUserNutritionalProfileController {
    private final ViewUserNutritionalProfileService service;

    public ViewUserNutritionalProfileController(ViewUserNutritionalProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<UserProfileResponseDTO> getProfile(@AuthenticationPrincipal Jwt authenticatedUser) {
        return ResponseEntity.ok(service.getProfile(authenticatedUser.getSubject()));
    }
}
