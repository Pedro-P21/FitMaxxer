package com.pedropacheco.nutrition.backend.userNutritionalProfile.service;

import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.UserProfileMapper;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.dto.UserProfileResponseDTO;
import com.pedropacheco.nutrition.backend.userNutritionalProfile.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViewUserNutritionalProfileService {
    private final UserProfileRepository repository;
    private final UserProfileMapper mapper;

    public ViewUserNutritionalProfileService(UserProfileRepository repository, UserProfileMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public UserProfileResponseDTO getProfile(String authenticatedUserId) {
        var profile = repository.findByUserId(new UserId(authenticatedUserId))
                .orElseThrow(UserProfileNotFoundException::new);
        return mapper.toProfileResponseDTO(profile);
    }
}
