package com.sonarshowcase;

import com.sonarshowcase.controller.AuthController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AuthController - intentionally vulnerable JWT controller
 */
class AuthControllerTest {

    private final AuthController controller = new AuthController();

    @Test
    void testLogin_validCredentials() {
        String username = "testuser";
        String password = "testpass";

        ResponseEntity<Map<String, String>> response = controller.login(username, password);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("token"));
        assertTrue(response.getBody().containsKey("message"));
    }

    @Test
    void testLogin_emptyUsername() {
        String username = "";
        String password = "testpass";

        ResponseEntity<Map<String, String>> response = controller.login(username, password);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"));
    }

    @Test
    void testLogin_emptyPassword() {
        String username = "testuser";
        String password = "";

        ResponseEntity<Map<String, String>> response = controller.login(username, password);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testLogin_bothEmpty() {
        String username = "";
        String password = "";

        ResponseEntity<Map<String, String>> response = controller.login(username, password);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testVerifyToken_validToken() {
        // First login to get a valid token
        ResponseEntity<Map<String, String>> loginResponse = controller.login("user", "pass");
        String token = "Bearer " + loginResponse.getBody().get("token");

        ResponseEntity<Map<String, String>> response = controller.verifyToken(token);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void testVerifyToken_withoutBearerPrefix() {
        // Get a token
        ResponseEntity<Map<String, String>> loginResponse = controller.login("user", "pass");
        String token = loginResponse.getBody().get("token");

        ResponseEntity<Map<String, String>> response = controller.verifyToken(token);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testVerifyToken_invalidToken() {
        String invalidToken = "invalid.token.here";

        ResponseEntity<Map<String, String>> response = controller.verifyToken(invalidToken);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testVerifyToken_emptyToken() {
        String emptyToken = "";

        ResponseEntity<Map<String, String>> response = controller.verifyToken(emptyToken);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testVerifyToken_noneAlgorithm() {
        // Create a token with "none" algorithm (vulnerability demonstration)
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"admin\",\"role\":\"admin\"}";

        String encodedHeader = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(header.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String encodedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        String noneToken = encodedHeader + "." + encodedPayload + ".";

        ResponseEntity<Map<String, String>> response = controller.verifyToken(noneToken);

        // Should accept "none" algorithm (vulnerability)
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().get("message").contains("none"));
    }

    @Test
    void testLogin_specialCharacters() {
        String username = "user@domain.com";
        String password = "p@$$w0rd!";

        ResponseEntity<Map<String, String>> response = controller.login(username, password);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody().get("token"));
    }
}
