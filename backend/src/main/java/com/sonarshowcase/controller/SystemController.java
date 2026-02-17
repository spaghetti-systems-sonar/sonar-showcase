package com.sonarshowcase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * System controller with Command Injection vulnerability.
 *
 * SEC-10: Command Injection - OS command injection
 *
 * @author SonarShowcase
 */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "System operations API endpoints. ⚠️ Contains intentional Command Injection vulnerabilities for demonstration.")
public class SystemController {

    /**
     * Default constructor for SystemController.
     */
    public SystemController() {
    }

    /**
     * SEC-10: Command Injection vulnerability - S2076, S4823
     * Executes ping command with unsanitized user input
     *
     * Attack vectors:
     * - Command chaining: host=google.com; cat /etc/passwd
     * - Command substitution: host=google.com`whoami`
     * - Pipe commands: host=google.com | ls -la /
     *
     * @param host Host to ping (no sanitization)
     * @return Ping command output
     */
    @Operation(
        summary = "Ping host (VULNERABLE)",
        description = "🔴 COMMAND INJECTION VULNERABILITY - Host parameter directly concatenated into shell command. " +
                     "Attacker can execute arbitrary OS commands. " +
                     "Attack examples: ?host=google.com; cat /etc/passwd or ?host=google.com | whoami"
    )
    @ApiResponse(responseCode = "200", description = "Ping output")
    @ApiResponse(responseCode = "500", description = "Command execution error")
    @GetMapping("/ping")
    public ResponseEntity<String> ping(
            @Parameter(description = "Host to ping (vulnerable to command injection)",
                      example = "google.com")
            @RequestParam String host) {
        try {
            // SEC: Direct concatenation of user input into shell command!
            // SHOULD USE: ProcessBuilder with arguments array, or sanitize input
            String command = "ping -c 4 " + host;

            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            process.waitFor();
            reader.close();

            return ResponseEntity.ok("Ping result:\n" + output.toString());

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error executing ping: " + e.getMessage());
        }
    }

    /**
     * SEC-10: Another command injection in DNS lookup
     *
     * @param domain Domain to lookup
     * @return DNS lookup output
     */
    @Operation(
        summary = "DNS lookup (VULNERABLE)",
        description = "🔴 COMMAND INJECTION VULNERABILITY - Domain parameter used in nslookup command without sanitization."
    )
    @GetMapping("/dns")
    public ResponseEntity<String> dnsLookup(
            @Parameter(description = "Domain to lookup (vulnerable to command injection)",
                      example = "google.com")
            @RequestParam String domain) {
        try {
            // SEC: Command injection via string concatenation
            String command = "nslookup " + domain;
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            return ResponseEntity.ok("DNS lookup:\n" + output.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("DNS error: " + e.getMessage());
        }
    }

    /**
     * SEC-10: Command injection in file compression
     *
     * @param filename File to compress
     * @return Compression result
     */
    @Operation(
        summary = "Compress file (VULNERABLE)",
        description = "🔴 COMMAND INJECTION VULNERABILITY - Filename parameter used in tar command without validation."
    )
    @PostMapping("/compress")
    public ResponseEntity<String> compressFile(
            @Parameter(description = "File to compress (vulnerable to command injection)")
            @RequestParam String filename) {
        try {
            // SEC: Command injection via shell execution
            String[] cmd = {"/bin/sh", "-c", "tar -czf /tmp/" + filename + ".tar.gz " + filename};
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();

            return ResponseEntity.ok("File compressed: " + filename + ".tar.gz");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Compression error: " + e.getMessage());
        }
    }
}
