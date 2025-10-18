package com.boardify.boardify_service.auth.controller;


import com.boardify.boardify_service.auth.dto.*;
import com.boardify.boardify_service.auth.entity.RefreshToken;
import com.boardify.boardify_service.user.repository.UserRepository;
import com.boardify.boardify_service.auth.jwt.JwtService;
import com.boardify.boardify_service.auth.service.RefreshTokenService;
import com.boardify.boardify_service.user.entity.Role;
import com.boardify.boardify_service.user.entity.UserEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticationManager authManager, JwtService jwt, UserRepository users, PasswordEncoder encoder, UserRepository userRepository, RefreshTokenService refreshTokenService) {
        this.authManager = authManager; this.jwt = jwt; this.users = users; this.encoder = encoder; this.userRepository=userRepository; this.refreshTokenService=refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody SignupRequest req) {
        if (users.existsByEmail(req.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Email already in use"));
        }

        // create new user
        UserEntity u = new UserEntity();
        u.setEmail(req.getEmail());
        u.setPassword(encoder.encode(req.getPassword()));
        u.setRoles(Set.of(Role.USER));
        users.save(u);

        // generate access token
        String accessToken = jwt.generateToken(u.getEmail());

        // generate refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(u);

        // build secure HttpOnly cookie
        ResponseCookie cookie = buildRefreshCookie(refreshToken.getToken(),60*2);

        // return access token in body, refresh token in cookie
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(accessToken));
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        var authToken = new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword());
        authManager.authenticate(authToken);

        // get the user from db
        UserEntity user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // generate access token
        String accessToken = jwt.generateToken(user.getEmail());

        // generate refresh token
        var refreshToken = refreshTokenService.createRefreshToken(user);

        ResponseCookie cookie = buildRefreshCookie(refreshToken.getToken(), 60*2);

        // return both tokens
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(accessToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(value = "refreshToken", required = false) String oldToken) {
        if (oldToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Missing refresh token"));
        }

        var opt = refreshTokenService.validateRefreshToken(oldToken);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Invalid or expired refresh token"));
        }

        var rt = opt.get();
        UserEntity user = rt.getUser();

        // generate new access token
        String newAccessToken = jwt.generateToken(user.getEmail());

        // rotate refresh token
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(rt);

        // build new HttpOnly cookie
        ResponseCookie cookie = buildRefreshCookie(newRefreshToken.getToken(), 60*2);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenRefreshResponse(newAccessToken)); // refresh token stays in cookie only
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                                            @RequestBody @Valid ChangePasswordRequest req) {
        UserEntity user = users.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(req.currentPassword(), user.getPassword())) {
            return ResponseEntity.status(400).body(new MessageResponse("Current password is incorrect"));
        }

        // Optional: enforce password policy (length, complexity)
        user.setPassword(encoder.encode(req.newPassword()));
        users.save(user);

        // Revoke all refresh tokens for this user (force re-login everywhere)
        refreshTokenService.deleteAllForUser(user.getId());

        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }



    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Invalidate refresh token
        if (refreshToken != null) {
            refreshTokenService.deleteByToken(refreshToken);
        }
        
        // Blacklist access token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            long exp = jwt.getExpiration(token);
            jwt.blacklistToken(token, exp);
        }

        // Invalidate the cookie by setting maxAge=0
        ResponseCookie expiredCookie = buildRefreshCookie("", 0);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    // helper method
    private ResponseCookie buildRefreshCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from("refreshToken", token)
            .httpOnly(true)
            .secure(false)               // set false only in local dev if no HTTPS
            .path("/auth")
            .sameSite("Strict")
            .maxAge(maxAgeSeconds)
            .build();
    }
}
