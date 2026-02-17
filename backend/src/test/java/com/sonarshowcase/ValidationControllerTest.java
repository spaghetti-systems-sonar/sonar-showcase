package com.sonarshowcase;

import com.sonarshowcase.controller.ValidationController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationController - intentionally vulnerable ReDoS controller
 */
class ValidationControllerTest {

    private final ValidationController controller = new ValidationController();

    @Test
    void testValidateEmail_validEmail() {
        String validEmail = "test@example.com";

        ResponseEntity<String> response = controller.validateEmail(validEmail);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("true"));
    }

    @Test
    void testValidateEmail_invalidEmail() {
        String invalidEmail = "notanemail";

        ResponseEntity<String> response = controller.validateEmail(invalidEmail);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("false"));
    }

    @Test
    void testValidateEmail_emptyEmail() {
        String emptyEmail = "";

        ResponseEntity<String> response = controller.validateEmail(emptyEmail);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testValidateUrl_validUrl() {
        String validUrl = "http://example.com";

        ResponseEntity<String> response = controller.validateUrl(validUrl);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testValidateUrl_invalidUrl() {
        String invalidUrl = "not a url";

        ResponseEntity<String> response = controller.validateUrl(invalidUrl);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("false"));
    }

    @Test
    void testValidateUrl_emptyUrl() {
        String emptyUrl = "";

        ResponseEntity<String> response = controller.validateUrl(emptyUrl);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testValidatePassword_validPassword() {
        String validPassword = "Test123!";

        ResponseEntity<String> response = controller.validatePassword(validPassword);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("true"));
    }

    @Test
    void testValidatePassword_invalidPassword() {
        String invalidPassword = "weak";

        ResponseEntity<String> response = controller.validatePassword(invalidPassword);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("false"));
    }

    @Test
    void testValidatePassword_emptyPassword() {
        String emptyPassword = "";

        ResponseEntity<String> response = controller.validatePassword(emptyPassword);

        assertEquals(200, response.getStatusCode().value());
    }
}
