package com.pedropacheco.nutrition.backend.dailyLog.service;

import java.time.YearMonth;

/** Shared eligibility rules for browsing history and using it as a copy source. */
final class LogHistoryWindow {
    private LogHistoryWindow() {}

    static void validateMonth(YearMonth requested, YearMonth current, int maxLookbackYears) {
        if (maxLookbackYears < 1) throw new IllegalArgumentException("History lookback must be at least one year.");
        if (requested.isAfter(current))
            throw new IllegalArgumentException("Daily log history cannot be requested for a future month.");
        if (requested.isBefore(current.minusYears(maxLookbackYears)))
            throw new IllegalArgumentException("The requested month is outside the supported history window.");
    }
}
