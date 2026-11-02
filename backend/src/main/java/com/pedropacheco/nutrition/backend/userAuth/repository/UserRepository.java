package com.pedropacheco.nutrition.backend.userAuth.repository;

import com.pedropacheco.nutrition.backend.userAuth.domain.Email;
import com.pedropacheco.nutrition.backend.userAuth.domain.User;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

   Boolean findByEmailOrUserId(Email email, UserId userId);

    @Query("SELECT u FROM User u WHERE u.email.email = :identifier OR u.userId.value = :identifier")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);
}
