package com.pedropacheco.nutrition.backend.userAuth.domain;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //primary key for the DB (differs from UserID business attribute)

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, unique = true))
    private Email email;

    @Embedded
    @AttributeOverride(name = "hashedValue", column = @Column(name = "password_hash", nullable = false))
    private Password password;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "user_id", nullable = false, unique = true))
    private UserId userId;

    protected User() {}

    public User(Email email, Password password, UserId userId) {
        this.email = Objects.requireNonNull(email, "Email must not be null");
        this.password = Objects.requireNonNull(password, "Password must not be null");
        this.userId = Objects.requireNonNull(userId, "UserId must not be null");
    }

    public Long getId() { return id; }
    public Email getEmail() { return email; }
    public Password getPassword() { return password; }
    public UserId getUserId() { return userId; }
}
