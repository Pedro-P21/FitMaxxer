package com.pedropacheco.nutrition.backend.userAuth.domain;

import jakarta.persistence.*;

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
    @AttributeOverride(name = "value", column = @Column(name = "userid", nullable = false, unique = true))
    private UserId userId;

    protected User() {}

    public User(Email email, Password password, UserId userId) {
        if (email == null) {
            throw new IllegalArgumentException("Email must not be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId must not be null");
        }

        this.email = email;
        this.password = password;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public Email getEmail() { return email; }
    public Password getPassword() { return password; }
    public UserId getUserId() { return userId; }
}
