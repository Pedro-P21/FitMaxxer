package com.pedropacheco.nutrition.backend.userAuth.controller;

import com.pedropacheco.nutrition.backend.userAuth.dto.LoginRequestDTO;
import com.pedropacheco.nutrition.backend.userAuth.dto.LoginResponseDTO;
import com.pedropacheco.nutrition.backend.userAuth.dto.RegisterRequestDTO;
import com.pedropacheco.nutrition.backend.userAuth.dto.UserAuthMapper;
import com.pedropacheco.nutrition.backend.userAuth.service.UserAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class UserAuthController {

    private final UserAuthService authService;
    private final UserAuthMapper loginMapper;

    public UserAuthController(UserAuthService authService, UserAuthMapper loginMapper) {
        this.authService = authService;
        this.loginMapper = loginMapper;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(@RequestBody RegisterRequestDTO requestDTO) {
        authService.registerUser(
                requestDTO.getEmail(),
                requestDTO.getPassword(),
                requestDTO.getUserId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO requestDTO) {
        String token = authService.authenticate(
                requestDTO.getIdentifier(),
                requestDTO.getPassword()
        );

        LoginResponseDTO responseDTO = loginMapper.toDTO(token);

        return ResponseEntity.ok(responseDTO);
    }
}