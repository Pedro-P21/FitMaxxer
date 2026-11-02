package com.pedropacheco.nutrition.backend.userAuth.domain;

import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class Password {
    private String hashedValue;

    protected Password() {}

    public Password(String hashedValue) {
        if(hashedValue == null || hashedValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid password. It cannot be null or empty");
        }
        this.hashedValue = hashedValue.trim();
    }

    public String getHashedValue() {
        return hashedValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Password password = (Password) o;
        return hashedValue.equals(password.hashedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hashedValue);
    }
}
