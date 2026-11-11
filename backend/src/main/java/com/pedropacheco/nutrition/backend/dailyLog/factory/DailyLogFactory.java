package com.pedropacheco.nutrition.backend.dailyLog.factory;

import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyGoalSnapshot;
import com.pedropacheco.nutrition.backend.dailyLog.domain.DailyLog;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class DailyLogFactory {
    public DailyLog create(UserId userId, LocalDate date, Long profileId, DailyGoalSnapshot goalSnapshot) {
        return new DailyLog(userId, date, profileId, goalSnapshot);
    }
}
