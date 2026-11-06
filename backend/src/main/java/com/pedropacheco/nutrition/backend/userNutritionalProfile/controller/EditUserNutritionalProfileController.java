package com.pedropacheco.nutrition.backend.userNutritionalProfile.controller;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.ProfileChangesDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalResponseDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.EditUserNutritionalProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
public class EditUserNutritionalProfileController {

    private final EditUserNutritionalProfileService editUserNutritionalProfileService;

    public EditUserNutritionalProfileController(EditUserNutritionalProfileService editUserNutritionalProfileService) {
        this.editUserNutritionalProfileService = editUserNutritionalProfileService;
    }

    /** Applies the authenticated user's confirmed profile changes. */
    @PutMapping
    public ResponseEntity<NutritionalGoalResponseDTO> editProfile(
            @AuthenticationPrincipal Jwt authenticatedUser,
            @RequestBody ProfileChangesDTO changesDTO
    ) {
        NutritionalGoalResponseDTO response =
        editUserNutritionalProfileService.editUserProfile(authenticatedUser.getSubject(), changesDTO);


        return ResponseEntity.ok(response);
    }
}
