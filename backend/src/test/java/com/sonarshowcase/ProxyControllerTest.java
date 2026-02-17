package com.sonarshowcase;

import com.sonarshowcase.controller.ProxyController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProxyController - intentionally vulnerable SSRF controller
 */
class ProxyControllerTest {

    private final ProxyController controller = new ProxyController();

    @Test
    void testFetchUrl_invalidUrl() {
        String invalidUrl = "not-a-valid-url";

        ResponseEntity<String> response = controller.fetchUrl(invalidUrl);

        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody().contains("Error"));
    }

    @Test
    void testFetchUrl_invalidProtocol() {
        String invalidUrl = "ftp://example.com";

        ResponseEntity<String> response = controller.fetchUrl(invalidUrl);

        // Will fail because we don't support FTP
        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void testRegisterWebhook_invalidUrl() {
        String invalidUrl = "not-a-url";

        ResponseEntity<String> response = controller.registerWebhook(invalidUrl);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testRegisterWebhook_unreachableUrl() {
        String unreachableUrl = "http://localhost:99999/webhook";

        ResponseEntity<String> response = controller.registerWebhook(unreachableUrl);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testProxyImage_invalidUrl() {
        String invalidUrl = "not-a-url";

        ResponseEntity<String> response = controller.proxyImage(invalidUrl);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testProxyImage_malformedUrl() {
        String malformedUrl = "http://";

        ResponseEntity<String> response = controller.proxyImage(malformedUrl);

        // The controller returns 200 with image metadata even for malformed URLs
        assertNotNull(response);
        assertNotNull(response.getBody());
    }
}
