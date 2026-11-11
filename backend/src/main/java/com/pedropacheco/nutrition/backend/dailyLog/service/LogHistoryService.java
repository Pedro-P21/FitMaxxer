package com.pedropacheco.nutrition.backend.dailyLog.service;

import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyLog;
import com.pedropacheco.nutrition.backend.dailyLog.dto.LogHistoryResponseDTO;
import com.pedropacheco.nutrition.backend.dailyLog.dto.PastHistoryMapper;
import com.pedropacheco.nutrition.backend.dailyLog.repository.DailyLogRepository;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class LogHistoryService {
    private final DailyLogRepository dailyLogRepository;
    private final PastHistoryMapper mapper;
    private final Clock clock;
    private final int maxLookbackYears;

    public LogHistoryService(DailyLogRepository dailyLogRepository,
                             PastHistoryMapper mapper,
                             Clock clock,
                             @Value("${daily-log.history.max-lookback-years:10}") int maxLookbackYears) {
        if (maxLookbackYears < 1) {
            throw new IllegalArgumentException("History lookback must be at least one year.");
        }
        this.dailyLogRepository = dailyLogRepository;
        this.mapper = mapper;
        this.clock = clock;
        this.maxLookbackYears = maxLookbackYears;
    }

    @Transactional(readOnly = true)
    public LogHistoryResponseDTO getPastHistory(String authenticatedUserId, int month, int year) {
        UserId userId = new UserId(authenticatedUserId);
        LocalDate today = LocalDate.now(clock);
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth requestedMonth;

        try {
            requestedMonth = YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Month and year must be valid calendar values.", exception);
        }

        if (requestedMonth.isAfter(currentMonth)) {
            throw new IllegalArgumentException("Daily log history cannot be requested for a future month.");
        }
        if (requestedMonth.isBefore(currentMonth.minusYears(maxLookbackYears))) {
            throw new IllegalArgumentException("The requested month is outside the supported history window.");
        }

        LocalDate startDate = requestedMonth.atDay(1);
        LocalDate endDate = requestedMonth.atEndOfMonth();
        if (requestedMonth.equals(currentMonth)) {
            endDate = today.minusDays(1);
        }

        List<DailyLog> logs = startDate.isAfter(endDate) ? List.of()
                : dailyLogRepository.findByUserIdAndDateRange(userId.getValue(), startDate, endDate);
        return mapper.toResponseDTO(logs, year, month);
    }
}
