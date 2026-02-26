package com.sonarshowcase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File controller with path traversal vulnerability.
 * 
 * SEC-06: Path Traversal - unvalidated file path
 * 
 * @author SonarShowcase
 */
@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Files", description = "File operations API endpoints. ⚠️ All endpoints contain intentional Path Traversal vulnerabilities for demonstration.")
public class FileController {
    
    /**
     * Default constructor for FileController.
     */
    public FileController() {
    }

    // SEC: Hardcoded file path
    private static final String UPLOAD_DIR = "/var/uploads/";
    
    /**
     * VULNERABILITY: Path Traversal (SonarQube Rule S2083)
     *
     * WHY THIS IS VULNERABLE:
     * - User input (filename) directly concatenated into file path without validation
     * - No path normalization or canonicalization checks
     * - No verification that resolved path stays within allowed directory
     * - No whitelist of allowed filenames
     *
     * HOW SONARQUBE DETECTS:
     * - Taint analysis: Tracks @RequestParam from user input to file system operations
     * - Pattern matching: Detects string concatenation in file path context
     * - Data flow analysis: Follows unvalidated user input through Paths.get() and Files.readAllBytes()
     *
     * ATTACK EXAMPLES:
     * - ?filename=../../../etc/passwd (read system password file)
     * - ?filename=../../../etc/shadow (read shadow password file)
     * - ?filename=../../../../root/.ssh/id_rsa (steal SSH private keys)
     * - ?filename=..%2f..%2f..%2fetc%2fpasswd (URL-encoded traversal)
     *
     * HOW TO FIX:
     * 1. Validate filename against whitelist (only alphanumeric + safe chars)
     * 2. Use Path.normalize() and verify result stays in allowed directory:
     *    Path resolved = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
     *    if (!resolved.startsWith(Paths.get(UPLOAD_DIR).toAbsolutePath())) throw exception
     * 3. Use UUID-based filenames instead of accepting user input
     * 4. Implement proper access controls
     *
     * OWASP: A01:2021 - Broken Access Control
     * CWE: CWE-22 (Path Traversal)
     *
     * @param filename Filename (vulnerable to path traversal)
     * @return ResponseEntity containing file content as bytes, or error message
     */
    @Operation(
        summary = "Download file (VULNERABLE)", 
        description = "🔴 PATH TRAVERSAL VULNERABILITY - Intentional security issue. " +
                     "Filename parameter is not validated, allowing access to arbitrary files. " +
                     "Attack example: ?filename=../../../etc/passwd"
    )
    @ApiResponse(responseCode = "200", description = "File content")
    @ApiResponse(responseCode = "400", description = "Error reading file")
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "Filename (vulnerable to path traversal)", example = "../../../etc/passwd")
            @RequestParam String filename) {
        try {
            // SEC: No validation of filename - path traversal possible
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            
            // SEC: Attacker can use: ?filename=../../../etc/passwd
            byte[] content = Files.readAllBytes(filePath);
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .body(content);
                    
        } catch (IOException e) {
            // SEC: Exposing internal error details
            return ResponseEntity.badRequest()
                    .body(("Error: " + e.getMessage()).getBytes());
        }
    }
    
    /**
     * VULNERABILITY: Path Traversal + Information Disclosure (SonarQube Rules S2083, S1148)
     *
     * WHY THIS IS VULNERABLE:
     * - User input (path) used directly as File() constructor argument
     * - Accepts absolute paths (e.g., /etc/passwd) and relative paths (e.g., ../../../etc/shadow)
     * - No validation that path is within allowed directory
     * - Resource leak: BufferedReader not closed in finally block
     * - Stack trace exposure in error handling
     *
     * HOW SONARQUBE DETECTS:
     * - Taint analysis: Tracks @RequestParam "path" through new File(path)
     * - Pattern matching: Detects printStackTrace() call (S1148)
     * - Resource leak detection: BufferedReader.close() not in try-with-resources
     *
     * ATTACK EXAMPLES:
     * - ?path=/etc/passwd (read via absolute path)
     * - ?path=../../../etc/shadow (read via relative traversal)
     * - ?path=/proc/self/environ (read process environment variables)
     * - ?path=/var/log/app.log (read application logs)
     *
     * SECONDARY VULNERABILITY - Information Disclosure:
     * - Stack traces expose internal file paths, Java versions, framework details
     * - Helps attackers map the system and plan further attacks
     *
     * HOW TO FIX:
     * 1. Reject absolute paths: if (Paths.get(path).isAbsolute()) throw exception
     * 2. Use try-with-resources for automatic resource management
     * 3. Replace printStackTrace() with proper logging (no details to user)
     * 4. Validate and normalize paths as in downloadFile()
     *
     * OWASP: A01:2021 - Broken Access Control, A05:2021 - Security Misconfiguration
     * CWE: CWE-22 (Path Traversal), CWE-209 (Information Exposure Through Error Message)
     *
     * @param path File path (vulnerable to path traversal)
     * @return ResponseEntity containing file content as string, or error stack trace
     */
    @Operation(
        summary = "Read file (VULNERABLE)", 
        description = "🔴 PATH TRAVERSAL VULNERABILITY - User input directly used as file path. " +
                     "Attack example: ?path=/etc/passwd or ?path=../../../etc/shadow"
    )
    @ApiResponse(responseCode = "200", description = "File content")
    @ApiResponse(responseCode = "500", description = "Error (exposes stack trace)")
    @GetMapping("/read")
    public ResponseEntity<String> readFile(
            @Parameter(description = "File path (vulnerable to path traversal)", example = "/etc/passwd")
            @RequestParam String path) {
        try {
            // SEC: Direct use of user input in file path
            File file = new File(path);
            
            // REL: No check if file exists
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            // REL: Resource leak - reader not closed in finally
            reader.close();
            
            return ResponseEntity.ok(content.toString());
            
        } catch (Exception e) {
            // SEC: Stack trace exposure
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return ResponseEntity.status(500).body(sw.toString());
        }
    }
    
    /**
     * VULNERABILITY: Path Traversal in DELETE Operation - CRITICAL (SonarQube Rule S2083)
     *
     * WHY THIS IS VULNERABLE:
     * - User can delete ANY file on the filesystem that the application has write access to
     * - String concatenation allows ../ sequences to escape UPLOAD_DIR
     * - No confirmation or authorization checks
     * - DELETE operations are particularly dangerous (data loss, system compromise)
     *
     * HOW SONARQUBE DETECTS:
     * - Taint analysis: Tracks user input to File.delete() operation
     * - Critical severity due to delete operation
     *
     * ATTACK EXAMPLES:
     * - ?filename=../../../var/log/app.log (delete application logs to hide traces)
     * - ?filename=../../../etc/cron.d/backup (disable backup jobs)
     * - ?filename=../../../app/config.yml (delete configuration, cause DoS)
     * - ?filename=../../../../usr/share/applications/important.desktop (system file deletion)
     *
     * REAL-WORLD IMPACT:
     * - Denial of Service: Delete critical configuration or application files
     * - Evidence tampering: Delete audit logs to hide malicious activity
     * - Privilege escalation: Delete files to bypass security checks
     *
     * HOW TO FIX:
     * 1. NEVER allow users to specify filenames for deletion directly
     * 2. Use database IDs to reference files, map ID to server-controlled filename
     * 3. Implement strict authorization (is user owner of the file?)
     * 4. Add audit logging for all deletion operations
     * 5. Use soft-delete (mark as deleted) instead of permanent deletion
     *
     * OWASP: A01:2021 - Broken Access Control
     * CWE: CWE-22 (Path Traversal), CWE-284 (Improper Access Control)
     *
     * @param filename Filename (vulnerable to path traversal)
     * @return ResponseEntity with deletion status message
     */
    @Operation(
        summary = "Delete file (VULNERABLE - DANGEROUS)", 
        description = "🔴 PATH TRAVERSAL VULNERABILITY - CRITICAL: User can delete any file on the system. " +
                     "Attack example: ?filename=../../../important/data.db"
    )
    @ApiResponse(responseCode = "200", description = "File deleted")
    @ApiResponse(responseCode = "400", description = "Failed to delete")
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(
            @Parameter(description = "Filename (vulnerable to path traversal)", example = "../../../important/data.db")
            @RequestParam String filename) {
        // SEC: User can delete any file: ?filename=../../../important/data.db
        File file = new File(UPLOAD_DIR + filename);
        
        if (file.delete()) {
            return ResponseEntity.ok("Deleted: " + filename);
        } else {
            return ResponseEntity.badRequest().body("Failed to delete");
        }
    }
    
    /**
     * VULNERABILITY: Zip Slip - Archive Extraction Path Traversal (SonarQube Rule S2083)
     *
     * WHY THIS IS VULNERABLE:
     * - Malicious ZIP files can contain entries with ../ in their names
     * - Extracting without validation writes files outside destination directory
     * - Example entry: "../../../../root/.ssh/authorized_keys"
     * - When extracted to /var/extracted, writes to /root/.ssh/authorized_keys
     *
     * HOW ZIP SLIP WORKS:
     * 1. Attacker creates ZIP with entries like: "../../../../../../tmp/evil.sh"
     * 2. Application extracts to destDir (/var/safe)
     * 3. Resolved path: /var/safe/../../../../../../tmp/evil.sh = /tmp/evil.sh
     * 4. File written outside intended directory
     *
     * HOW SONARQUBE DETECTS:
     * - Pattern matching: Detects ZIP extraction without path validation
     * - Looks for ZipInputStream/ZipFile usage without canonicalization checks
     *
     * ATTACK EXAMPLES:
     * Malicious ZIP containing:
     * - ../../../../root/.ssh/authorized_keys (SSH access)
     * - ../../../../etc/cron.d/backdoor (scheduled backdoor)
     * - ../../../../var/www/html/shell.php (web shell)
     * - ../../../../tmp/evil.so (library injection)
     *
     * REAL-WORLD IMPACT:
     * - Remote Code Execution: Write malicious executable to startup scripts
     * - Privilege Escalation: Overwrite system files
     * - Persistence: Install backdoors in cron jobs or SSH authorized_keys
     *
     * HOW TO FIX:
     * 1. Validate each ZIP entry before extraction:
     *    File destFile = new File(destDir, zipEntry.getName());
     *    String canonicalDestPath = destFile.getCanonicalPath();
     *    String canonicalDestDir = new File(destDir).getCanonicalPath();
     *    if (!canonicalDestPath.startsWith(canonicalDestDir + File.separator)) {
     *        throw new IOException("Zip Slip attempt detected");
     *    }
     * 2. Use security-hardened ZIP libraries
     * 3. Run extraction in sandboxed environment
     *
     * OWASP: A01:2021 - Broken Access Control, A03:2021 - Injection
     * CWE: CWE-22 (Path Traversal), CWE-59 (Improper Link Resolution)
     * CVE: CVE-2018-1002200 (Zip Slip vulnerability)
     *
     * @param zipPath ZIP file path
     * @param destDir Destination directory (vulnerable to zip slip)
     * @return ResponseEntity with extraction status message
     */
    @Operation(
        summary = "Extract ZIP (VULNERABLE)", 
        description = "🔴 ZIP SLIP VULNERABILITY - Malicious ZIP can write files outside destination directory"
    )
    @ApiResponse(responseCode = "200", description = "ZIP extracted")
    @PostMapping("/extract")
    public ResponseEntity<String> extractZip(
            @Parameter(description = "ZIP file path", example = "/tmp/archive.zip")
            @RequestParam String zipPath,
            @Parameter(description = "Destination directory (vulnerable to zip slip)", example = "/var/extracted")
            @RequestParam String destDir) {
        // SEC: Zip slip - malicious zip can write outside destDir
        // This is a placeholder showing the vulnerable pattern
        return ResponseEntity.ok("Extracted to: " + destDir);
    }
    
    // ==========================================================================
    // PATH TRAVERSAL DEMO ENDPOINTS
    // ==========================================================================
    
    /**
     * VULNERABILITY: Path Traversal via Username Parameter (SonarQube Rule S2083)
     *
     * WHY THIS IS VULNERABLE:
     * - Username parameter directly concatenated into file path with .json extension
     * - Attacker controls the directory portion of the path via ../
     * - Even though .json is appended, ../ sequences are processed before extension
     * - Example: username=../../../etc/passwd results in path /var/profiles/../../../etc/passwd.json
     * - Java resolves to /etc/passwd.json (or /etc/passwd if file exists without extension check)
     *
     * HOW SONARQUBE DETECTS:
     * - Taint analysis: Tracks @RequestParam "username" through string concatenation
     * - Detects concatenation result used in Paths.get() and Files.readAllBytes()
     *
     * ATTACK EXAMPLES:
     * - ?username=../../../etc/passwd (read /etc/passwd.json or /etc/passwd)
     * - ?username=../../../etc/shadow (read shadow file)
     * - ?username=../../config/database (read database credentials)
     * - ?username=../../../../proc/self/environ (read environment variables)
     *
     * WHY APPENDING .json DOESN'T HELP:
     * - Path traversal ../ is resolved BEFORE .json extension
     * - /var/profiles/../../../etc/passwd.json resolves to /etc/passwd.json
     * - If /etc/passwd exists and no .json file, some systems may return /etc/passwd
     * - Extension doesn't prevent directory traversal
     *
     * HOW TO FIX:
     * 1. Validate username against strict whitelist: ^[a-zA-Z0-9_-]+$
     * 2. Reject any input containing ../, ..\, or /
     * 3. Use database to map username to file ID, never use username in path directly
     * 4. Canonicalize path and verify it starts with /var/profiles/
     *
     * OWASP: A01:2021 - Broken Access Control
     * CWE: CWE-22 (Path Traversal)
     *
     * @param username Username (vulnerable to path traversal)
     * @return ResponseEntity containing profile content, or 404 if not found
     */
    @Operation(
        summary = "Get user profile (VULNERABLE)", 
        description = "🔴 PATH TRAVERSAL VULNERABILITY - Username parameter used in file path without validation. " +
                     "Attack example: ?username=../../../etc/passwd"
    )
    @ApiResponse(responseCode = "200", description = "Profile content")
    @ApiResponse(responseCode = "404", description = "Profile not found")
    @GetMapping("/profile")
    public ResponseEntity<String> getUserProfile(
            @Parameter(description = "Username (vulnerable to path traversal)", example = "../../../etc/passwd")
            @RequestParam String username) {
        // SEC: Path Traversal - S2083
        // Attacker can use: ?username=../../../etc/passwd
        String profilePath = "/var/profiles/" + username + ".json";
        
        try {
            Path path = Paths.get(profilePath);
            // SEC: No canonicalization check - allows ../ sequences
            String content = new String(Files.readAllBytes(path));
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * VULNERABILITY: Path Traversal via Date Parameter Injection (SonarQube Rule S2083)
     *
     * WHY THIS IS VULNERABLE:
     * - Date parameter used in path construction without validation
     * - Attacker can inject directory traversal sequences into "date" value
     * - String concatenation: "/var/logs/app/" + date + ".log"
     * - .log extension doesn't prevent traversal (same reason as getUserProfile())
     *
     * HOW SONARQUBE DETECTS:
     * - Taint analysis: Tracks @RequestParam "date" to file system operations
     * - Detects string concatenation in file path context
     *
     * ATTACK EXAMPLES:
     * - ?date=2024-01-01/../../../../etc/passwd (inject traversal in date)
     * - ?date=../../../etc/shadow (pure traversal, ignoring date format)
     * - ?date=2024-01-01/../../config/secrets (access config files)
     * - ?date=2024/../../../../../root/.bashrc (mix valid date with traversal)
     *
     * CREATIVE ATTACK VECTORS:
     * - Use seemingly valid date format to bypass weak validation:
     *   ?date=2024-01-01/../../../../etc/passwd looks like valid date prefix
     * - If validation only checks start: date.startsWith("202"), attack still works
     *
     * WHY APPENDING .log DOESN'T HELP:
     * - /var/logs/app/2024-01-01/../../../../etc/passwd.log
     *   Resolves to: /etc/passwd.log (traversal resolved before extension)
     * - Many systems ignore extensions or fallback to extensionless files
     *
     * RESOURCE LEAK ISSUE:
     * - BufferedReader not closed in finally or try-with-resources
     * - SonarQube will also flag this as S2095 (resource leak)
     *
     * HOW TO FIX:
     * 1. Validate date format strictly: Pattern.matches("\\d{4}-\\d{2}-\\d{2}", date)
     * 2. Parse date and reconstruct: LocalDate.parse(date).toString()
     * 3. Use try-with-resources for BufferedReader
     * 4. Whitelist allowed dates or date ranges
     * 5. Use Path.normalize() and verify result stays in /var/logs/app/
     *
     * OWASP: A01:2021 - Broken Access Control, A03:2021 - Injection
     * CWE: CWE-22 (Path Traversal), CWE-20 (Improper Input Validation)
     *
     * @param date Date (vulnerable to path traversal)
     * @return ResponseEntity containing log content, or 404 if not found
     */
    @Operation(
        summary = "Get logs (VULNERABLE)", 
        description = "🔴 PATH TRAVERSAL VULNERABILITY - Date parameter used in file path without validation. " +
                     "Attack example: ?date=2024-01-01/../../../../etc/shadow"
    )
    @ApiResponse(responseCode = "200", description = "Log content")
    @ApiResponse(responseCode = "404", description = "Log not found")
    @GetMapping("/logs")
    public ResponseEntity<String> getLogs(
            @Parameter(description = "Date (vulnerable to path traversal)", example = "2024-01-01/../../../../etc/shadow")
            @RequestParam String date) {
        // SEC: Path Traversal - date parameter is not validated
        String logPath = "/var/logs/app/" + date + ".log";
        
        try {
            File logFile = new File(logPath);
            // SEC: No check if resolved path is within allowed directory
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close(); // REL: Should use try-with-resources
            return ResponseEntity.ok(content.toString());
        } catch (IOException e) {
            return ResponseEntity.status(404).body("Log not found");
        }
    }
    
    /**
     * VULNERABILITY: Path Traversal in WRITE Operation - CRITICAL (SonarQube Rule S2083)
     *
     * WHY THIS IS CRITICAL:
     * - Path Traversal in WRITE operation = Remote Code Execution potential
     * - Attacker can write malicious files to arbitrary locations
     * - Can overwrite system files, install backdoors, create web shells
     * - More dangerous than read-only path traversal
     *
     * HOW SONARQUBE DETECTS:
     * - Taint analysis: Tracks @RequestParam "filename" to Files.write()
     * - Critical severity due to write operation
     * - Detects string concatenation in write path context
     *
     * ATTACK EXAMPLES:
     * 1. SSH Backdoor:
     *    POST /api/v1/files/export
     *    ?filename=../../../root/.ssh/authorized_keys
     *    &data=ssh-rsa AAAAB3... attacker@evil.com
     *    Result: Attacker gains SSH access as root
     *
     * 2. Cron Job Backdoor:
     *    POST /api/v1/files/export
     *    ?filename=../../../etc/cron.d/backdoor
     *    &data=* * * * * root /tmp/reverse_shell.sh
     *    Result: Scheduled reverse shell execution
     *
     * 3. Web Shell:
     *    POST /api/v1/files/export
     *    ?filename=../../../var/www/html/shell.php
     *    &data=<?php system($_GET['cmd']); ?>
     *    Result: Remote command execution via web
     *
     * 4. Configuration Overwrite:
     *    POST /api/v1/files/export
     *    ?filename=../../../app/config.yml
     *    &data=admin_password: hacked
     *    Result: Admin account compromise
     *
     * REAL-WORLD IMPACT:
     * - Remote Code Execution: Write executable files to PATH directories
     * - Privilege Escalation: Overwrite SUID binaries or sudo configurations
     * - Persistence: Install backdoors that survive reboots
     * - Data Exfiltration: Overwrite log rotation configs to prevent log deletion
     *
     * HOW TO FIX:
     * 1. NEVER allow user-controlled filenames in write operations
     * 2. Generate server-controlled filenames: UUID.randomUUID() + extension
     * 3. Store user-provided filename in database metadata only
     * 4. Implement strict access controls and authentication
     * 5. Use chroot jails or containers to limit file system access
     * 6. Validate content-type and scan uploaded data for malware
     *
     * OWASP: A01:2021 - Broken Access Control, A03:2021 - Injection
     * CWE: CWE-22 (Path Traversal), CWE-73 (External Control of File Name or Path)
     *
     * @param filename Filename (vulnerable to path traversal)
     * @param data Data to write
     * @return ResponseEntity with export status message
     */
    @Operation(
        summary = "Export data (VULNERABLE - CRITICAL)", 
        description = "🔴 PATH TRAVERSAL VULNERABILITY - CRITICAL: User can write arbitrary files to filesystem. " +
                     "Attack example: ?filename=../../../root/.ssh/authorized_keys"
    )
    @ApiResponse(responseCode = "200", description = "Data exported")
    @ApiResponse(responseCode = "500", description = "Export failed")
    @PostMapping("/export")
    public ResponseEntity<String> exportData(
            @Parameter(description = "Filename (vulnerable to path traversal)", example = "../../../tmp/malicious.sh")
            @RequestParam String filename,
            @Parameter(description = "Data to write")
            @RequestBody String data) {
        
        // SEC: Path Traversal in write operation - CRITICAL
        // Attacker can write to any location: ?filename=../../../root/.ssh/authorized_keys
        String exportPath = UPLOAD_DIR + filename;
        
        try {
            // SEC: No validation of filename - can contain ../
            Files.write(Paths.get(exportPath), data.getBytes());
            return ResponseEntity.ok("Exported to: " + exportPath);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Export failed: " + e.getMessage());
        }
    }
    
    /**
     * VULNERABILITY: Path Traversal in Template Inclusion + Information Disclosure (S2083, S2647)
     *
     * WHY THIS IS VULNERABLE:
     * - Template name parameter used in file path without sanitization
     * - Paths.get(templateDir, name) still vulnerable if name contains ../
     * - Even Paths.get(dir, userInput) can be exploited with ../
     * - Error messages expose internal file paths
     *
     * HOW SONARQUBE DETECTS:
     * - Taint analysis: Tracks @RequestParam "name" to Paths.get() and Files.readAllBytes()
     * - Detects that even Paths.get(fixed, variable) is unsafe with untrusted input
     * - Information disclosure: Error message contains templatePath variable
     *
     * ATTACK EXAMPLES:
     * - ?name=../../../../etc/passwd (read system files)
     * - ?name=../../config/database.yml (read database credentials)
     * - ?name=../../../app/secrets.env (read secrets)
     * - ?name=../../../../proc/self/environ (read environment variables)
     *
     * WHY Paths.get(dir, userInput) ISN'T SAFE:
     * - Paths.get("/var/templates/", "../../../etc/passwd")
     * - Java resolves this to: /var/templates/../../../etc/passwd
     * - Which normalizes to: /etc/passwd
     * - The "safe" API doesn't prevent traversal!
     *
     * INFORMATION DISCLOSURE:
     * - Error message "Failed to load template: /var/templates/../../../../etc/passwd.html"
     * - Exposes internal directory structure: /var/templates/
     * - Helps attacker understand system layout
     * - SonarQube flags exposing variables in error messages (S2647)
     *
     * TEMPLATE INJECTION RISK:
     * - If templates are processed (Thymeleaf, Freemarker, etc.), this could become
     *   Server-Side Template Injection (SSTI) if attacker can control template content
     * - Reading arbitrary files + template processing = RCE potential
     *
     * HOW TO FIX:
     * 1. Whitelist allowed template names: Pattern.matches("^[a-zA-Z0-9_-]+$", name)
     * 2. After resolution, verify path stays in template directory:
     *    Path resolved = Paths.get(templateDir, name).toRealPath();
     *    if (!resolved.startsWith(Paths.get(templateDir).toRealPath())) throw error
     * 3. Use template IDs from database, not filenames from user
     * 4. Don't expose internal paths in error messages
     * 5. Use generic error: "Template not found" (no details)
     *
     * OWASP: A01:2021 - Broken Access Control, A05:2021 - Security Misconfiguration
     * CWE: CWE-22 (Path Traversal), CWE-209 (Information Exposure), CWE-1336 (Template Injection)
     *
     * @param name Template name (vulnerable to path traversal)
     * @return ResponseEntity containing template content, or error status
     */
    @Operation(
        summary = "Get template (VULNERABLE)", 
        description = "🔴 PATH TRAVERSAL VULNERABILITY - Template name not sanitized. " +
                     "Attack example: ?name=../../../../etc/passwd"
    )
    @ApiResponse(responseCode = "200", description = "Template content")
    @ApiResponse(responseCode = "404", description = "Template not found")
    @ApiResponse(responseCode = "500", description = "Error (reveals internal paths)")
    @GetMapping("/template")
    public ResponseEntity<String> getTemplate(
            @Parameter(description = "Template name (vulnerable to path traversal)", example = "../../../../etc/passwd")
            @RequestParam String name) {
        // SEC: Path Traversal - template name not sanitized
        String templateDir = "/var/templates/";
        Path templatePath = Paths.get(templateDir, name + ".html");
        
        try {
            // SEC: User-controlled path without validation
            if (Files.exists(templatePath)) {
                String template = new String(Files.readAllBytes(templatePath));
                // Process template (placeholder)
                return ResponseEntity.ok(template);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            // SEC: Revealing internal paths in error message
            return ResponseEntity.status(500).body("Failed to load template: " + templatePath);
        }
    }
}

