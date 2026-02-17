package com.sonarshowcase;

import com.sonarshowcase.controller.DataImportController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DataImportController - intentionally vulnerable deserialization controller
 */
class DataImportControllerTest {

    private final DataImportController controller = new DataImportController();

    @Test
    void testImportData_validSerializedString() throws Exception {
        // Create a valid serialized string
        String testString = "test";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(testString);
        oos.close();

        String encoded = Base64.getEncoder().encodeToString(bos.toByteArray());

        ResponseEntity<String> response = controller.importData(encoded);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("String"));
    }

    @Test
    void testImportData_invalidBase64() {
        String invalidData = "not-valid-base64!@#$";

        ResponseEntity<String> response = controller.importData(invalidData);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testImportData_emptyData() {
        String emptyData = "";

        ResponseEntity<String> response = controller.importData(emptyData);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testRestoreSession_validSerializedData() throws Exception {
        String testString = "session123";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(testString);
        oos.close();

        String encoded = Base64.getEncoder().encodeToString(bos.toByteArray());

        ResponseEntity<String> response = controller.restoreSession(encoded);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testRestoreSession_invalidData() {
        String invalidData = "invalid";

        ResponseEntity<String> response = controller.restoreSession(invalidData);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testCreateTestData_defaultText() {
        ResponseEntity<String> response = controller.createTestData("test");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    void testCreateTestData_customText() {
        ResponseEntity<String> response = controller.createTestData("custom data");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void testCreateTestData_emptyText() {
        ResponseEntity<String> response = controller.createTestData("");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }
}
