package com.pedropacheco.nutrition.backend.dailyLog.domain;

import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class LogDate {
    private LocalDate value;

    protected LogDate() {}

    public LogDate(LocalDate value) {
        this.value = Objects.requireNonNull(value, "Log date cannot be null.");
    }

    public LocalDate getValue() { return value; }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof LogDate that && value.equals(that.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
}
