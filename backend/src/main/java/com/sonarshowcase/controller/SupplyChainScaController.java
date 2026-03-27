package com.sonarshowcase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Supply-chain security demo: calls into vulnerable {@code log4j-core} 2.14.1 from application code.
 *
 * <p>SEC: CVE-2021-44228 (Log4Shell) — untrusted input must not be passed to Log4j2 message APIs.
 */
@RestController
@RequestMapping("/api/v1/sca-demo")
@Tag(
        name = "Supply chain demo",
        description = "Intentional SCA reachability endpoints. Unsafe against untrusted input; demo only."
)
public class SupplyChainScaController {

    private static final Logger LOG4J_DEMO = LogManager.getLogger(SupplyChainScaController.class);

    /**
     * SEC: Log4Shell pattern — user-controlled string flows into Log4j2 {@code Logger.info} (CVE-2021-44228).
     *
     * @param userInput request body (plain text)
     * @return minimal acknowledgment
     */
    @Operation(
            summary = "Echo input through Log4j 2.14.1 (VULNERABLE — CVE-2021-44228)",
            description = "Logs the request body via org.apache.logging.log4j.Logger for supply-chain / "
                    + "static-analysis reachability. Do not expose to untrusted users."
    )
    @PostMapping(value = "/log4j-echo", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> log4jEcho(@RequestBody(required = false) String userInput) {
        String payload = userInput != null ? userInput : "";
        LOG4J_DEMO.info("{}", payload);
        return ResponseEntity.ok("logged");
    }
}
