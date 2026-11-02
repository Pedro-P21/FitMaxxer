package com.pedropacheco.nutrition.backend.userAuth.factory;

import com.pedropacheco.nutrition.backend.userAuth.domain.Email;
import com.pedropacheco.nutrition.backend.userAuth.domain.Password;
import com.pedropacheco.nutrition.backend.userAuth.domain.User;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {
    //Creators assume that passwords are already hashed! They must be validated beforehand!
    public User createNewUser(String rawEmail, String hashedPassword, String rawUserId) {
        // Triggers internal validations
        Email emailObj = new Email(rawEmail);
        Password passwordObj = new Password(hashedPassword);
        UserId userIdObj = new UserId(rawUserId);

        return new User(emailObj, passwordObj, userIdObj);
    }

    public Email createEmail(String rawEmail) {
        return new Email(rawEmail);
    }
    public UserId createUserId(String rawUserId) {
        return new UserId(rawUserId);
    }
}

