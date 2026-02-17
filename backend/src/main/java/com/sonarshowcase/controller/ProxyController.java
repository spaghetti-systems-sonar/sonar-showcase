package com.sonarshowcase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Proxy controller with SSRF vulnerability.
 *
 * SEC-09: SSRF - Server-Side Request Forgery
 *
 * @author SonarShowcase
 */
@RestController
@RequestMapping("/api/v1/proxy")
@Tag(name = "Proxy", description = "Proxy/Fetch API endpoints. ⚠️ Contains intentional SSRF vulnerabilities for demonstration.")
public class ProxyController {

    /**
     * Default constructor for ProxyController.
     */
    public ProxyController() {
    }

    /**
     * SEC-09: SSRF vulnerability - S5144
     * Fetches content from user-provided URL without validation
     *
     * Attack vectors:
     * - Access internal services: http://localhost:8080/actuator/health
     * - Scan internal network: http://192.168.1.1:22
     * - Access cloud metadata: http://169.254.169.254/latest/meta-data/
     * - Read local files: file:///etc/passwd
     *
     * @param url URL to fetch (no validation)
     * @return Fetched content or error message
     */
    @Operation(
        summary = "Fetch URL content (VULNERABLE)",
        description = "🔴 SSRF VULNERABILITY - Fetches content from any URL without validation. " +
                     "Attacker can access internal services, scan networks, or access cloud metadata. " +
                     "Attack examples: ?url=http://localhost:8080/api/v1/health or ?url=http://169.254.169.254/latest/meta-data/"
    )
    @ApiResponse(responseCode = "200", description = "Fetched content")
    @ApiResponse(responseCode = "500", description = "Fetch error")
    @GetMapping("/fetch")
    public ResponseEntity<String> fetchUrl(
            @Parameter(description = "URL to fetch (vulnerable to SSRF)",
                      example = "http://localhost:8080/api/v1/health")
            @RequestParam String url) {
        try {
            // SEC: No URL validation - SSRF vulnerability!
            // SHOULD VALIDATE:
            // - Whitelist allowed domains
            // - Block private IP ranges (127.0.0.0/8, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
            // - Block metadata endpoints (169.254.169.254)
            // - Block file:// protocol

            URL targetUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) targetUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();

            return ResponseEntity.ok("Fetched from " + url + ":\n" + content.toString());

        } catch (Exception e) {
            // SEC: Exposing error details helps attacker enumerate internal services
            return ResponseEntity.status(500)
                    .body("Error fetching URL: " + e.getMessage());
        }
    }

    /**
     * SEC-09: SSRF via webhook registration
     *
     * @param webhookUrl Webhook URL to register
     * @return Registration status
     */
    @Operation(
        summary = "Register webhook (VULNERABLE)",
        description = "🔴 SSRF VULNERABILITY - Webhook URLs are not validated, allowing internal service access."
    )
    @PostMapping("/webhook")
    public ResponseEntity<String> registerWebhook(
            @Parameter(description = "Webhook URL (vulnerable to SSRF)")
            @RequestParam String webhookUrl) {
        try {
            // SEC: Test webhook immediately without validation
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.getOutputStream().write("{\"test\":true}".getBytes());

            int responseCode = conn.getResponseCode();
            return ResponseEntity.ok("Webhook registered and tested: " + webhookUrl +
                                   " (Response: " + responseCode + ")");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Webhook error: " + e.getMessage());
        }
    }

    /**
     * SEC-09: SSRF via image proxy
     *
     * @param imageUrl Image URL to proxy
     * @return Image metadata
     */
    @Operation(
        summary = "Proxy image (VULNERABLE)",
        description = "🔴 SSRF VULNERABILITY - Image URLs not validated, can access internal resources."
    )
    @GetMapping("/image")
    public ResponseEntity<String> proxyImage(
            @Parameter(description = "Image URL (vulnerable to SSRF)",
                      example = "http://192.168.1.1/admin")
            @RequestParam String imageUrl) {
        try {
            // SEC: No validation of image URL
            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            String contentType = conn.getContentType();
            int contentLength = conn.getContentLength();

            return ResponseEntity.ok("Image proxied - Type: " + contentType +
                                   ", Size: " + contentLength + " bytes");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Image proxy error: " + e.getMessage());
        }
    }
}
