package com.pedropacheco.nutrition.backend.dailyLog.dto;

import java.util.List;

public record LogHistoryResponseDTO(int year, int month, List<DailyLogResponseDTO> logs) {
    public LogHistoryResponseDTO {
        logs = List.copyOf(logs);
    }
}
