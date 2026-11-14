package com.pedropacheco.nutrition.backend.dailyLog.repository;

import com.pedropacheco.nutrition.backend.dailyLog.domain.MealItem;

/** Persistence operations for children removed through the DailyLog aggregate. */
public interface DailyLogItemRemoval {
    void deleteRemovedItem(MealItem item);
}
