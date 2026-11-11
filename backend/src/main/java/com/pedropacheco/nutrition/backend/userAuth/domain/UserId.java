package com.pedropacheco.nutrition.backend.userAuth.domain;

import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class UserId {
    private String userID;
    protected UserId() {}

    public UserId(String value) {
        if(value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid user id. It cannot be null or empty");
        }
        this.userID = value.trim();
    }

    public String getValue() {
        return userID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserId userId = (UserId) o;
        return Objects.equals(userID, userId.userID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID);
    }
}
