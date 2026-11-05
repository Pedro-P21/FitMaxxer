package com.pedropacheco.nutrition.backend.userNutritionalProfile.repository;

import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.UserNutritionalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserNutritionalProfile, Long> {


    Optional<UserNutritionalProfile> findByUserId(UserId userId);


    boolean existsByUserId(UserId userId);
}