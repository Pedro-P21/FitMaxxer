package com.pedropacheco.nutrition.backend.dailyLog.dto;

import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyLog;
import com.pedropacheco.nutrition.backend.dailyLog.service.NutrientScalingService;
import org.springframework.stereotype.Component;

import java.util.List;

/** Maps stored daily logs to history responses, deriving each summary from its log snapshots. */
@Component
public class PastHistoryMapper {
    private final DailyLogMapper dailyLogMapper;
    private final NutrientScalingService nutrientScalingService;

    public PastHistoryMapper(DailyLogMapper dailyLogMapper, NutrientScalingService nutrientScalingService) {
        this.dailyLogMapper = dailyLogMapper;
        this.nutrientScalingService = nutrientScalingService;
    }

    public LogHistoryResponseDTO toResponseDTO(List<DailyLog> logs, int year, int month) {
        List<DailyLogResponseDTO> responses = logs.stream()
                .map(log -> dailyLogMapper.toResponseDTO(log,
                        log.calculateNutritionSummary(nutrientScalingService)))
                .toList();
        return new LogHistoryResponseDTO(year, month, responses);
    }
}
