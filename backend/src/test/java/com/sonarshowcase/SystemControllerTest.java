package com.sonarshowcase;

import com.sonarshowcase.controller.SystemController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SystemController - intentionally vulnerable command injection controller
 */
class SystemControllerTest {

    private final SystemController controller = new SystemController();

    @Test
    void testPing_simpleHost() {
        String host = "127.0.0.1";

        ResponseEntity<String> response = controller.ping(host);

        // Might succeed or fail depending on system, but should not throw
        assertNotNull(response);
    }

    @Test
    void testPing_invalidHost() {
        String host = "this-host-does-not-exist-xyz123";

        ResponseEntity<String> response = controller.ping(host);

        // Should return some response (success or error)
        assertNotNull(response);
    }

    @Test
    void testPing_emptyHost() {
        String host = "";

        ResponseEntity<String> response = controller.ping(host);

        // Should handle empty host gracefully
        assertNotNull(response);
    }

    @Test
    void testDnsLookup_validDomain() {
        String domain = "localhost";

        ResponseEntity<String> response = controller.dnsLookup(domain);

        assertNotNull(response);
    }

    @Test
    void testDnsLookup_invalidDomain() {
        String domain = "this-domain-definitely-does-not-exist-12345";

        ResponseEntity<String> response = controller.dnsLookup(domain);

        // Should return error or result
        assertNotNull(response);
    }

    @Test
    void testCompressFile_simpleFilename() {
        String filename = "testfile.txt";

        ResponseEntity<String> response = controller.compressFile(filename);

        // Will likely fail since file doesn't exist, but should handle gracefully
        assertNotNull(response);
    }

    @Test
    void testCompressFile_emptyFilename() {
        String filename = "";

        ResponseEntity<String> response = controller.compressFile(filename);

        assertNotNull(response);
    }
}
