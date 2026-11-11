package com.pedropacheco.nutrition.backend.dailyLog.repository;

import com.pedropacheco.nutrition.backend.cli.TerminalRunner;
import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyGoalSnapshot;
import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyLog;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DailyLogRepositoryTest {
    @Autowired private DailyLogRepository repository;
    @MockitoBean private TerminalRunner terminalRunner;

    @BeforeEach
    void clearLogs() {
        repository.deleteAll();
    }

    @Test
    void findsOnlyOwnerLogsWithinRequestedMonthNewestFirst() {
        repository.save(new DailyLog(new UserId("owner-1"), LocalDate.of(2026, 10, 1), 1L,
                new DailyGoalSnapshot(2000, 120, 220, 70)));
        repository.save(new DailyLog(new UserId("owner-1"), LocalDate.of(2026, 10, 31), 1L,
                new DailyGoalSnapshot(2000, 120, 220, 70)));
        repository.save(new DailyLog(new UserId("owner-1"), LocalDate.of(2026, 11, 1), 1L,
                new DailyGoalSnapshot(2000, 120, 220, 70)));
        repository.save(new DailyLog(new UserId("owner-2"), LocalDate.of(2026, 10, 15), 2L,
                new DailyGoalSnapshot(2000, 120, 220, 70)));

        var logs = repository.findByUserIdAndDateRange("owner-1",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertEquals(2, logs.size());
        assertEquals(LocalDate.of(2026, 10, 31), logs.get(0).getDate().getValue());
        assertEquals(LocalDate.of(2026, 10, 1), logs.get(1).getDate().getValue());
    }
}
