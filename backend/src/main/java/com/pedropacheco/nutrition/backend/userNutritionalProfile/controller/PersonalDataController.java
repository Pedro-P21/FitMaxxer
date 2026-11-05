package com.pedropacheco.nutrition.backend.userNutritionalProfile.controller;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalRequestDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.NutritionalGoalResponseDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.service.PersonalDataService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
public class PersonalDataController {

    private final PersonalDataService personalDataService;

    public PersonalDataController(PersonalDataService personalDataService) {
        this.personalDataService = personalDataService;
    }

    /**
     * Endpoint responsible for logging personal data and retrieving information about the user's nutritional goals.
     */
    @PostMapping("/personal-data")
    public ResponseEntity<NutritionalGoalResponseDTO> logPersonalData(
            @AuthenticationPrincipal Jwt authenticatedUser,
            @RequestBody NutritionalGoalRequestDTO requestDTO
    ) {

        NutritionalGoalResponseDTO responseDTO = personalDataService.logPersonalData(authenticatedUser.getSubject(), requestDTO);
        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }
}
