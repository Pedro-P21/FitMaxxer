package com.pedropacheco.nutrition.backend.dailyLog.repository;

import com.pedropacheco.nutrition.backend.dailyLog.domain.MealItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class DailyLogItemRemovalImpl implements DailyLogItemRemoval {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void deleteRemovedItem(MealItem item) {
        entityManager.remove(item);
    }
}
