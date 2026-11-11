package com.pedropacheco.nutrition.backend.dailyLog.controller;

import com.pedropacheco.nutrition.backend.dailyLog.dto.DailyLogResponseDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.AddMealItemRequestDTO;
import com.pedropacheco.nutrition.backend.dailyLog.service.DailyLogApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daily-log")
public class DailyLogController {
    private final DailyLogApplicationService dailyLogApplicationService;

    public DailyLogController(DailyLogApplicationService dailyLogApplicationService) {
        this.dailyLogApplicationService = dailyLogApplicationService;
    }

    @GetMapping("/today")
    public ResponseEntity<DailyLogResponseDTO> getToday(@AuthenticationPrincipal Jwt authenticatedUser) {
        return ResponseEntity.ok(dailyLogApplicationService.getToday(authenticatedUser.getSubject()));
    }

    @PostMapping("/today/items")
    public ResponseEntity<DailyLogResponseDTO> addMealItem(@AuthenticationPrincipal Jwt authenticatedUser,
                                                            @RequestBody AddMealItemRequestDTO request) {
        return ResponseEntity.ok(dailyLogApplicationService.addMealItem(authenticatedUser.getSubject(), request));
    }
}
