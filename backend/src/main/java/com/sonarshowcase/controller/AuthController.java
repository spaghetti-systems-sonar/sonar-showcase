package com.sonarshowcase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication controller with JWT vulnerabilities.
 *
 * SEC-14: JWT Vulnerabilities - Weak signing, "none" algorithm, no expiration
 *
 * @author SonarShowcase
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication API endpoints. ⚠️ Contains intentional JWT vulnerabilities for demonstration.")
public class AuthController {

    /**
     * Default constructor for AuthController.
     */
    public AuthController() {
    }

    // SEC: Hardcoded weak JWT secret
    private static final String JWT_SECRET = "weak";

    /**
     * SEC-14: JWT with weak secret and "none" algorithm acceptance - S5659
     *
     * @param username Username
     * @param password Password
     * @return JWT token
     */
    @Operation(
        summary = "Login and get JWT (VULNERABLE)",
        description = "🔴 JWT VULNERABILITIES - Multiple issues: " +
                     "1. Weak signing key ('weak'), " +
                     "2. No expiration time, " +
                     "3. Accepts 'none' algorithm. " +
                     "Attack: Modify token with alg='none' or brute-force weak secret."
    )
    @ApiResponse(responseCode = "200", description = "JWT token issued")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Parameter(description = "Username") @RequestParam String username,
            @Parameter(description = "Password") @RequestParam String password) {

        // SEC: No real authentication check
        if (username.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing credentials"));
        }

        // SEC: Create JWT with multiple vulnerabilities
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"" + username + "\",\"role\":\"user\"}";

        // SEC: No expiration time (should include "exp" claim)

        String encodedHeader = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        // SEC: Weak secret "weak" - easily brute-forced
        String signature = simpleHmacSha256(encodedHeader + "." + encodedPayload, JWT_SECRET);
        String token = encodedHeader + "." + encodedPayload + "." + signature;

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("message", "Login successful");

        return ResponseEntity.ok(response);
    }

    /**
     * SEC-14: JWT verification that accepts "none" algorithm
     *
     * @param token JWT token to verify
     * @return User information if token is valid
     */
    @Operation(
        summary = "Verify JWT (VULNERABLE)",
        description = "🔴 JWT VULNERABILITY - Accepts tokens with 'none' algorithm (no signature). " +
                     "Attack: Create token with {\"alg\":\"none\"} and no signature part."
    )
    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyToken(
            @Parameter(description = "JWT token (vulnerable to 'none' algorithm attack)")
            @RequestHeader("Authorization") String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid token format"));
            }

            // Decode header
            String headerJson = new String(
                Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);

            // SEC: Accepts "none" algorithm - CRITICAL vulnerability!
            if (headerJson.contains("\"none\"")) {
                // VULNERABLE: Token with "none" algorithm is accepted without signature verification
                String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

                return ResponseEntity.ok(Map.of(
                    "message", "Token verified (using 'none' algorithm - INSECURE!)",
                    "payload", payloadJson
                ));
            }

            // SEC: No expiration check
            // SHOULD CHECK: "exp" claim and compare with current time

            // SEC: Weak signature verification (simplified for demo)
            if (parts.length >= 3) {
                String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

                return ResponseEntity.ok(Map.of(
                    "message", "Token verified",
                    "payload", payloadJson
                ));
            }

            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token verification failed: " + e.getMessage()));
        }
    }

    /**
     * Simple HMAC-SHA256 implementation for demo purposes
     */
    private String simpleHmacSha256(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey =
                new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error computing HMAC", e);
        }
    }
}
