package com.sonarshowcase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * XML controller with XXE vulnerability.
 *
 * SEC-08: XXE Injection - XML External Entity attack
 *
 * @author SonarShowcase
 */
@RestController
@RequestMapping("/api/v1/xml")
@Tag(name = "XML", description = "XML processing API endpoints. ⚠️ Contains intentional XXE vulnerabilities for demonstration.")
public class XmlController {

    /**
     * Default constructor for XmlController.
     */
    public XmlController() {
    }

    /**
     * SEC-08: XXE vulnerability - S2755, S4829
     * Parses XML without disabling external entities
     *
     * <p>Attack payload example:</p>
     * <pre>{@code
     * <?xml version="1.0"?>
     * <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
     * <root><data>&xxe;</data></root>
     * }</pre>
     *
     * @param xmlContent XML content to parse
     * @return Parsed XML content or error message
     */
    @Operation(
        summary = "Parse XML (VULNERABLE)",
        description = "🔴 XXE VULNERABILITY - XML parser does not disable external entities. " +
                     "Attacker can read arbitrary files from the server. " +
                     "Attack example: Send XML with <!DOCTYPE> containing SYSTEM entity pointing to file:///etc/passwd"
    )
    @ApiResponse(responseCode = "200", description = "Parsed XML content")
    @ApiResponse(responseCode = "400", description = "Parsing error")
    @PostMapping("/parse")
    public ResponseEntity<String> parseXml(
            @Parameter(description = "XML content (vulnerable to XXE)",
                      example = "<?xml version=\"1.0\"?>\n<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>\n<root><data>&xxe;</data></root>")
            @RequestBody String xmlContent) {
        try {
            // SEC: No XXE protection - vulnerable!
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // SHOULD SET BUT DON'T (intentional vulnerability):
            // factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            // factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            // factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            // factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

            // Extract and return content (which may contain file contents via XXE)
            String result = doc.getDocumentElement().getTextContent();

            return ResponseEntity.ok("Parsed XML content: " + result);

        } catch (Exception e) {
            // SEC: Exposing error details
            return ResponseEntity.badRequest()
                    .body("XML parsing error: " + e.getMessage());
        }
    }

    /**
     * SEC-08: Another XXE variant - parsing XML config
     *
     * @param configXml Configuration XML
     * @return Configuration status
     */
    @Operation(
        summary = "Upload XML configuration (VULNERABLE)",
        description = "🔴 XXE VULNERABILITY - Configuration parser vulnerable to XXE attacks."
    )
    @PostMapping("/config")
    public ResponseEntity<String> uploadConfig(
            @RequestBody String configXml) {
        try {
            // SEC: Same XXE vulnerability
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(configXml)));

            return ResponseEntity.ok("Configuration uploaded: " + doc.getDocumentElement().getNodeName());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Config error: " + e.getMessage());
        }
    }
}
