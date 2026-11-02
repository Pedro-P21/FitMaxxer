package com.pedropacheco.nutrition.backend.userAuth.service;


import com.pedropacheco.nutrition.backend.userAuth.domain.User;
import com.pedropacheco.nutrition.backend.userAuth.factory.UserFactory;
import com.pedropacheco.nutrition.backend.userAuth.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class UserAuthService {
    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;

    public UserAuthService(UserRepository userRepository, UserFactory userFactory, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registerUser(String rawEmail, String rawPassword, String rawUserId) {
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User provisionalUser = userFactory.createNewUser(rawEmail, hashedPassword, rawUserId);

        boolean exists = userRepository.findByEmailOrUserId(
                provisionalUser.getEmail(),
                provisionalUser.getUserId()
        );

        if (exists) {
            throw new IllegalArgumentException("User with given email or ID already exists.");
        }

        userRepository.save(provisionalUser);
    }

    public String authenticate(String identifier, String rawPassword) {
        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials."));

        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPassword().getHashedValue());

        if (!passwordMatches) {
            throw new BadCredentialsException("Invalid credentials.");
        }

        return generateJwtToken(user);
    }

    private String generateJwtToken(User user) {
        return "mock-jwt-token-for-" + user.getUserId().getValue();
    }
}