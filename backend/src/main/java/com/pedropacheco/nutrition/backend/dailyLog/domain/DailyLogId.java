package com.pedropacheco.nutrition.backend.dailyLog.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class DailyLogId {
    @Column(name = "daily_log_id")
    private String value;

    protected DailyLogId() {}
    public DailyLogId(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Daily log ID cannot be blank.");
        this.value = value.trim();
    }
    public static DailyLogId generate() { return new DailyLogId(UUID.randomUUID().toString()); }
    public String getValue() { return value; }
    @Override public boolean equals(Object other) {
        return this == other || other instanceof DailyLogId that && value.equals(that.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
}
