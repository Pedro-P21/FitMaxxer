package com.pedropacheco.nutrition.backend.dailyLog.repository;

import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyLog;
import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyLogId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;

public interface DailyLogRepository extends JpaRepository<DailyLog, DailyLogId> {
    @Query("select log from DailyLog log where log.userId = :userId and log.date.value = :date")
    Optional<DailyLog> findByUserIdAndDate(@Param("userId") String userId, @Param("date") LocalDate date);
}
