package com.pedropacheco.nutrition.backend.dailyLog.controller;

import com.pedropacheco.nutrition.backend.dailyLog.dto.LogHistoryResponseDTO;
import com.pedropacheco.nutrition.backend.dailyLog.service.LogHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daily-log/history")
public class LogHistoryController {
    private final LogHistoryService logHistoryService;

    public LogHistoryController(LogHistoryService logHistoryService) {
        this.logHistoryService = logHistoryService;
    }

    @GetMapping
    public ResponseEntity<LogHistoryResponseDTO> getPastHistory(
            @AuthenticationPrincipal Jwt authenticatedUser,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(logHistoryService.getPastHistory(authenticatedUser.getSubject(), month, year));
    }
}
