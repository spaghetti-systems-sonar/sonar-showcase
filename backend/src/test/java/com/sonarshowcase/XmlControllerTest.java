package com.sonarshowcase;

import com.sonarshowcase.controller.XmlController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for XmlController - intentionally vulnerable XXE controller
 */
class XmlControllerTest {

    private final XmlController controller = new XmlController();

    @Test
    void testParseXml_validXml() {
        String validXml = "<?xml version=\"1.0\"?><root><data>test</data></root>";

        ResponseEntity<String> response = controller.parseXml(validXml);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("test"));
    }

    @Test
    void testParseXml_invalidXml() {
        String invalidXml = "not valid xml";

        ResponseEntity<String> response = controller.parseXml(invalidXml);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().contains("error"));
    }

    @Test
    void testParseXml_emptyXml() {
        String emptyXml = "";

        ResponseEntity<String> response = controller.parseXml(emptyXml);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testUploadConfig_validXml() {
        String validXml = "<?xml version=\"1.0\"?><config><setting>value</setting></config>";

        ResponseEntity<String> response = controller.uploadConfig(validXml);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("config"));
    }

    @Test
    void testUploadConfig_invalidXml() {
        String invalidXml = "invalid";

        ResponseEntity<String> response = controller.uploadConfig(invalidXml);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testParseXml_xxeAttempt() {
        String xxeXml = "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><root><data>&xxe;</data></root>";

        // This should parse (vulnerability demonstration)
        ResponseEntity<String> response = controller.parseXml(xxeXml);

        // The response code depends on whether the file exists
        assertNotNull(response);
    }
}
