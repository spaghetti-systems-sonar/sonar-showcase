package com.sonarshowcase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Validation controller with ReDoS vulnerability.
 *
 * SEC-13: ReDoS - Regular Expression Denial of Service
 *
 * @author SonarShowcase
 */
@RestController
@RequestMapping("/api/v1/validate")
@Tag(name = "Validation", description = "Input validation API endpoints. ⚠️ Contains intentional ReDoS vulnerabilities for demonstration.")
public class ValidationController {

    /**
     * Default constructor for ValidationController.
     */
    public ValidationController() {
    }

    /**
     * SEC-13: ReDoS vulnerability - S5852, S6019
     * Regex with catastrophic backtracking
     *
     * Attack: Send input like "aaaaaaaaaaaaaaaaaaaaX" causes exponential backtracking
     *
     * @param input Input to validate
     * @return Validation result
     */
    @Operation(
        summary = "Validate email format (VULNERABLE)",
        description = "🔴 ReDoS VULNERABILITY - Regex pattern has catastrophic backtracking. " +
                     "Attacker can cause DoS by sending input that triggers exponential backtracking. " +
                     "Attack example: Send 'aaaaaaaaaaaaaaaaaaaaX' to cause CPU exhaustion"
    )
    @ApiResponse(responseCode = "200", description = "Validation result")
    @GetMapping("/email")
    public ResponseEntity<String> validateEmail(
            @Parameter(description = "Email to validate (can trigger ReDoS)",
                      example = "test@example.com")
            @RequestParam String input) {

        // SEC: Catastrophic backtracking regex - ReDoS vulnerability!
        // Pattern: (a+)+ causes exponential time complexity
        Pattern pattern = Pattern.compile("^([a-zA-Z0-9]+)+@([a-zA-Z0-9]+)+\\.([a-zA-Z]{2,})+$");

        long startTime = System.currentTimeMillis();
        Matcher matcher = pattern.matcher(input);
        boolean isValid = matcher.matches();
        long endTime = System.currentTimeMillis();

        return ResponseEntity.ok("Email validation: " + isValid +
                               " (took " + (endTime - startTime) + "ms)");
    }

    /**
     * SEC-13: Another ReDoS - URL validation
     *
     * @param url URL to validate
     * @return Validation result
     */
    @Operation(
        summary = "Validate URL (VULNERABLE)",
        description = "🔴 ReDoS VULNERABILITY - Complex regex with nested quantifiers."
    )
    @GetMapping("/url")
    public ResponseEntity<String> validateUrl(
            @Parameter(description = "URL to validate (can trigger ReDoS)")
            @RequestParam String url) {

        // SEC: Another catastrophic backtracking pattern
        Pattern pattern = Pattern.compile("^(https?://)?([a-zA-Z0-9]+\\.)*[a-zA-Z0-9]+\\.[a-zA-Z]+(/.)*$");

        long startTime = System.currentTimeMillis();
        boolean isValid = pattern.matcher(url).matches();
        long endTime = System.currentTimeMillis();

        return ResponseEntity.ok("URL validation: " + isValid +
                               " (took " + (endTime - startTime) + "ms)");
    }

    /**
     * SEC-13: ReDoS in password complexity check
     *
     * @param password Password to validate
     * @return Validation result
     */
    @Operation(
        summary = "Validate password complexity (VULNERABLE)",
        description = "🔴 ReDoS VULNERABILITY - Password regex vulnerable to backtracking attacks."
    )
    @GetMapping("/password")
    public ResponseEntity<String> validatePassword(
            @Parameter(description = "Password to validate (can trigger ReDoS)")
            @RequestParam String password) {

        // SEC: Nested quantifiers causing ReDoS
        // Try: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaa!"
        Pattern pattern = Pattern.compile("^(?=.*[a-z]+)(?=.*[A-Z]+)(?=.*[0-9]+)(?=.*[!@#$%^&*]+).{8,}$");

        long startTime = System.currentTimeMillis();
        boolean isValid = pattern.matcher(password).matches();
        long endTime = System.currentTimeMillis();

        return ResponseEntity.ok("Password complexity validation: " + isValid +
                               " (took " + (endTime - startTime) + "ms)");
    }
}
