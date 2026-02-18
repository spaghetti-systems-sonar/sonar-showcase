package com.sonarshowcase.controller;

import com.sonarshowcase.dto.UserDto;
import com.sonarshowcase.model.User;
import com.sonarshowcase.repository.UserRepository;
import com.sonarshowcase.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.ArrayList;

/**
 * User controller with layer bypass and other issues
 * 
 * MNT: Layer bypass - controller directly uses repository
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management API endpoints. ⚠️ Contains intentional security vulnerabilities for demonstration.")
public class UserController {

    /**
     * Default constructor for Spring controller.
     * Spring will use this constructor and inject dependencies via @Autowired fields.
     */
    public UserController() {
        // Default constructor for Spring
    }

    @Autowired
    private UserService userService;
    
    // MNT: Layer bypass - controller should only talk to service
    @Autowired
    private UserRepository userRepository;
    
    // SEC: Direct EntityManager usage in controller bypasses all security layers
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Get all users - bypasses service layer
     * 
     * @return ResponseEntity containing a list of all users
     */
    @Operation(summary = "Get all users", description = "Returns all users. ⚠️ SECURITY: Returns passwords in plain text (intentional vulnerability)")
    @ApiResponse(responseCode = "200", description = "List of users (includes sensitive data)")
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        // MNT: Bypassing service layer, going directly to repository
        List<User> users = userRepository.findAll();
        
        // SEC: Returning full User entities including passwords
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get user by ID with NPE risk
     * 
     * @param id The user ID
     * @return ResponseEntity containing the user
     */
    @Operation(summary = "Get user by ID", description = "Returns a user by ID. ⚠️ SECURITY: Returns password in plain text. ⚠️ BUG: Throws exception if user not found (NPE risk)")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "500", description = "User not found (throws exception)")
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id) {
        // REL: NPE - .get() on Optional without check
        User user = userRepository.findById(id).get();
        return ResponseEntity.ok(user);
    }
    
    /**
     * Create user - mixes concerns
     * 
     * @param userDto The user data transfer object
     * @return ResponseEntity containing the created user
     */
    @Operation(
        summary = "Create new user", 
        description = "Creates a new user. ⚠️ SECURITY: Stores password in plain text. ⚠️ SECURITY: Role can be set by user input without validation. ⚠️ SECURITY: Logs password to console",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User data", 
            required = true, 
            content = @Content(schema = @Schema(implementation = UserDto.class))
        )
    )
    @ApiResponse(responseCode = "200", description = "User created (includes password in response)")
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody UserDto userDto) {
        // MNT: Business logic in controller (should be in service)
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        
        // SEC: Storing password without hashing
        user.setPassword(userDto.getPassword());
        
        // SEC: Setting role from user input without validation
        user.setRole(userDto.getRole());
        
        // MNT: Magic string
        if (user.getRole() == null) {
            user.setRole("USER");
        }
        
        User saved = userRepository.save(user);
        
        // MNT: Debug logging left in
        System.out.println("Created user: " + saved.getUsername() + " with password: " + saved.getPassword());
        
        return ResponseEntity.ok(saved);
    }
    
    /**
     * Search users - potential SQL injection passthrough
     * 
     * @param q The search query string
     * @return ResponseEntity containing a list of matching users
     */
    @Operation(summary = "Search users", description = "Searches users by username or email. ⚠️ MNT: Inefficient in-memory search. ⚠️ SECURITY: Returns passwords in plain text")
    @ApiResponse(responseCode = "200", description = "List of matching users")
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(
            @Parameter(description = "Search query", example = "john")
            @RequestParam String q) {
        // MNT: Single character variable name
        List<User> result = new ArrayList<>();
        
        // SEC: Passing unsanitized user input
        for (User u : userRepository.findAll()) {
            // MNT: Inefficient search in memory
            if (u.getUsername().contains(q) || u.getEmail().contains(q)) {
                result.add(u);
            }
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Delete user - no authorization check
     * 
     * @param id The user ID to delete
     * @return ResponseEntity with a success message
     */
    @Operation(summary = "Delete user", description = "Deletes a user by ID. ⚠️ SECURITY: No authorization check - anyone can delete any user")
    @ApiResponse(responseCode = "200", description = "User deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @Parameter(description = "User ID to delete", example = "1")
            @PathVariable Long id) {
        // SEC: No authorization - anyone can delete any user
        userRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }
    
    // ==========================================================================
    // SQL INJECTION DEMO ENDPOINTS
    // ==========================================================================
    
    /**
     * SEC-01: SQL Injection - Direct string concatenation in query
     * 
     * VULNERABLE ENDPOINT - Demonstrates SQL Injection
     * Attack example: {@code GET /api/v1/users/login?username=admin'--&password=anything}
     * This bypasses password check by commenting out the rest of the query
     * 
     * Another attack: {@code GET /api/v1/users/login?username=' OR '1'='1'--&password=x}
     * This returns the first user in the database
     */
    /**
     * Login endpoint with SQL injection vulnerability
     * 
     * @param username The username (vulnerable to SQL injection)
     * @param password The password (vulnerable to SQL injection)
     * @return ResponseEntity containing the user if login succeeds, or error message if it fails
     */
    @Operation(
        summary = "Login (VULNERABLE)", 
        description = "🔴 SQL INJECTION VULNERABILITY - Intentional security issue for demonstration. " +
                     "User input is directly concatenated into SQL query. " +
                     "Attack examples: username=admin'-- or username=' OR '1'='1'--"
    )
    @ApiResponse(responseCode = "200", description = "User found (vulnerable to SQL injection)")
    @ApiResponse(responseCode = "401", description = "Login failed")
    @GetMapping("/login")
    @SuppressWarnings("all")
    public ResponseEntity<?> loginUser(
            @Parameter(description = "Username (vulnerable to SQL injection)", example = "admin'--")
            @RequestParam String username,
            @Parameter(description = "Password (vulnerable to SQL injection)", example = "anything")
            @RequestParam String password) {
        
        // SEC: SQL Injection vulnerability - S3649
        // User input is directly concatenated into the SQL query without sanitization
        String sql = "SELECT * FROM users WHERE username = '" + username + 
                     "' AND password = '" + password + "'";
        
        System.out.println("DEBUG: Executing SQL: " + sql); // SEC: Logging SQL with user input
        
        try {
            User user = (User) entityManager.createNativeQuery(sql, User.class).getSingleResult();
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            // SEC: Revealing database error details to attacker
            return ResponseEntity.status(401).body("Login failed: " + e.getMessage());
        }
    }
    
    /**
     * SEC-01: SQL Injection in search functionality
     * 
     * VULNERABLE ENDPOINT - SQL Injection via LIKE clause
     * Attack example: GET /api/v1/users/vulnerable-search?term=' UNION SELECT * FROM users WHERE role='ADMIN'--
     */
    /**
     * Vulnerable search endpoint with SQL injection
     * 
     * @param term The search term (vulnerable to SQL injection)
     * @return ResponseEntity containing a list of matching users
     */
    @Operation(
        summary = "Vulnerable search (SQL INJECTION)", 
        description = "🔴 SQL INJECTION VULNERABILITY - User input directly concatenated into SQL LIKE clause. " +
                     "Attack example: ?term=' UNION SELECT * FROM users WHERE role='ADMIN'--"
    )
    @ApiResponse(responseCode = "200", description = "Search results (vulnerable to SQL injection)")
    @ApiResponse(responseCode = "400", description = "SQL error (may expose database structure)")
    @GetMapping("/vulnerable-search")
    @SuppressWarnings("all")
    public ResponseEntity<List<User>> vulnerableSearch(
            @Parameter(description = "Search term (vulnerable to SQL injection)", example = "' UNION SELECT * FROM users WHERE role='ADMIN'--")
            @RequestParam String term) {
        // SEC: SQL Injection - User input directly in LIKE clause
        String sql = "SELECT * FROM users WHERE username LIKE '%" + term + "%' " +
                     "OR email LIKE '%" + term + "%'";
        
        try {
            @SuppressWarnings("unchecked")
            List<User> users = entityManager.createNativeQuery(sql, User.class).getResultList();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }
    
    /**
     * SEC-01: SQL Injection via ORDER BY clause
     * 
     * VULNERABLE ENDPOINT - ORDER BY injection
     * Attack example: GET /api/v1/users/sorted?orderBy=username; DROP TABLE users;--
     */
    /**
     * Get sorted users with SQL injection vulnerability
     * 
     * @param orderBy The column to sort by (vulnerable to SQL injection)
     * @return ResponseEntity containing a sorted list of users
     */
    @Operation(
        summary = "Get sorted users (SQL INJECTION)", 
        description = "🔴 SQL INJECTION VULNERABILITY - ORDER BY clause uses user input directly. " +
                     "Attack example: ?orderBy=username; DROP TABLE users;--"
    )
    @ApiResponse(responseCode = "200", description = "Sorted list of users")
    @ApiResponse(responseCode = "500", description = "SQL error (may expose database structure)")
    @GetMapping("/sorted")
    @SuppressWarnings("all")
    public ResponseEntity<List<User>> getUsersSorted(
            @Parameter(description = "Column to sort by (vulnerable to SQL injection)", example = "username")
            @RequestParam(defaultValue = "id") String orderBy) {
        // SEC: SQL Injection - ORDER BY with user input
        String sql = "SELECT * FROM users ORDER BY " + orderBy;
        
        try {
            @SuppressWarnings("unchecked")
            List<User> users = entityManager.createNativeQuery(sql, User.class).getResultList();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            // SEC: Database structure exposed in error
            return ResponseEntity.status(500).body(null);
        }
    }
    
    /**
     * Update password - extremely insecure
     * 
     * @param id The user ID
     * @param oldPassword The old password (insecure - sent in URL)
     * @param newPassword The new password (insecure - sent in URL, no validation)
     * @return ResponseEntity with a success or error message
     */
    @Operation(
        summary = "Update password (INSECURE)", 
        description = "Updates user password. ⚠️ SECURITY: Passwords in URL parameters (logged). " +
                     "⚠️ SECURITY: Plain text password comparison. ⚠️ SECURITY: No password strength validation. " +
                     "⚠️ SECURITY: No authorization check"
    )
    @ApiResponse(responseCode = "200", description = "Password updated")
    @ApiResponse(responseCode = "400", description = "Wrong password (reveals user exists)")
    @PutMapping("/{id}/password")
    public ResponseEntity<String> updatePassword(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Old password (insecure - sent in URL)", example = "oldpass123")
            @RequestParam String oldPassword,
            @Parameter(description = "New password (insecure - sent in URL, no validation)", example = "newpass123")
            @RequestParam String newPassword) {
        
        // SEC: Password in URL parameters (logged in access logs)
        User user = userRepository.findById(id).get();
        
        // SEC: Plain text password comparison
        if (!user.getPassword().equals(oldPassword)) {
            // SEC: Reveals that user exists
            return ResponseEntity.badRequest().body("Wrong password");
        }
        
        // SEC: No password strength validation
        user.setPassword(newPassword);
        userRepository.save(user);
        
        return ResponseEntity.ok("Password updated");
    }

    /**
     * SEC-12: LDAP Injection vulnerability - S2078
     * LDAP filter constructed with unsanitized user input
     *
     * Attack examples:
     * - username=*)(uid=*))(|(uid=* (returns all users)
     * - username=admin)(|(password=*)) (bypasses authentication)
     *
     * @param username Username for LDAP search
     * @return LDAP search result
     */
    @Operation(
        summary = "LDAP user search (VULNERABLE)",
        description = "🔴 LDAP INJECTION VULNERABILITY - Username directly concatenated into LDAP filter. " +
                     "Attacker can manipulate LDAP queries. " +
                     "Attack example: ?username=*)(uid=*))(|(uid=* to retrieve all users"
    )
    @GetMapping("/ldap-search")
    public ResponseEntity<String> ldapSearch(
            @Parameter(description = "Username for LDAP search (vulnerable to LDAP injection)",
                      example = "john")
            @RequestParam String username) {
        try {
            // SEC: LDAP injection - user input directly in filter
            // SHOULD USE: Properly escaped LDAP filter or prepared statements
            String filter = "(&(objectClass=user)(uid=" + username + "))";

            // Simulated LDAP search (in real app would connect to LDAP server)
            // DirContext ctx = new InitialDirContext();
            // NamingEnumeration results = ctx.search("dc=example,dc=com", filter, controls);

            // For demo, just return the filter that would be used
            return ResponseEntity.ok("LDAP filter (VULNERABLE): " + filter +
                                   "\n\nIn a real app, this would execute against LDAP server." +
                                   "\n\nAttack example: username=*)(uid=*))(|(uid=* would return all users.");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("LDAP error: " + e.getMessage());
        }
    }

    /**
     * SEC: SQL Injection in INSERT statement
     *
     * @param username Username
     * @param email Email
     * @return Success message
     */
    @Operation(
        summary = "Insert user (VULNERABLE)",
        description = "🔴 SQL INJECTION in INSERT statement. " +
                     "Attack: username=admin', 'admin@example.com'); DROP TABLE users;--"
    )
    @PostMapping("/insert-unsafe")
    public ResponseEntity<String> insertUserUnsafe(
            @RequestParam String username,
            @RequestParam String email) {
        try {
            // SEC: SQL Injection in INSERT
            String sql = "INSERT INTO users (username, email, password, role, active, created_at) VALUES ('" +
                        username + "', '" + email + "', 'password123', 'user', true, CURRENT_TIMESTAMP)";
            entityManager.createNativeQuery(sql).executeUpdate();
            return ResponseEntity.ok("User inserted");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Insert error: " + e.getMessage());
        }
    }

    /**
     * SEC: SQL Injection in UPDATE statement
     *
     * @param id User ID
     * @param email New email
     * @return Success message
     */
    @Operation(
        summary = "Update email (VULNERABLE)",
        description = "🔴 SQL INJECTION in UPDATE statement. " +
                     "Attack: email=test@example.com', role='ADMIN' WHERE '1'='1"
    )
    @PutMapping("/{id}/email-unsafe")
    public ResponseEntity<String> updateEmailUnsafe(
            @PathVariable Long id,
            @RequestParam String email) {
        try {
            // SEC: SQL Injection in UPDATE
            String sql = "UPDATE users SET email = '" + email + "' WHERE id = " + id;
            entityManager.createNativeQuery(sql).executeUpdate();
            return ResponseEntity.ok("Email updated");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Update error: " + e.getMessage());
        }
    }

    /**
     * SEC: SQL Injection in LIMIT clause
     *
     * @param limit Limit value
     * @return List of users
     */
    @Operation(
        summary = "Get users with limit (VULNERABLE)",
        description = "🔴 SQL INJECTION in LIMIT clause. " +
                     "Attack: limit=1; DROP TABLE users;--"
    )
    @GetMapping("/with-limit")
    public ResponseEntity<List<User>> getUsersWithLimit(
            @RequestParam String limit) {
        try {
            // SEC: SQL Injection via LIMIT
            String sql = "SELECT * FROM users LIMIT " + limit;
            @SuppressWarnings("unchecked")
            List<User> users = entityManager.createNativeQuery(sql, User.class).getResultList();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * SEC-CSRF: Delete user account via GET request - CSRF vulnerability
     *
     * State-changing operation exposed via GET without CSRF protection.
     * Can be exploited via: {@literal <img src="http://localhost:8080/api/v1/users/1/delete-account">}
     *
     * @param id User ID to delete
     * @return Success message
     */
    @Operation(
        summary = "Delete account (CSRF VULNERABILITY)",
        description = "🔴 CSRF VULNERABILITY - State-changing operation via GET request. " +
                     "No CSRF token validation. Can be exploited via hidden image tag or link. " +
                     "Attack example: img tag with src pointing to delete-account endpoint"
    )
    @ApiResponse(responseCode = "200", description = "Account deleted")
    @GetMapping("/{id}/delete-account")
    public ResponseEntity<String> deleteAccountCSRF(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id) {
        // SEC-CSRF: Dangerous state-changing operation via GET
        // SEC-CSRF: No CSRF token validation
        // SEC-CSRF: No confirmation required
        User user = userRepository.findById(id).get();
        userRepository.delete(user);

        return ResponseEntity.ok("Account " + id + " deleted successfully");
    }

    /**
     * SEC-CSRF: Promote user to admin via GET - CSRF + Broken Access Control
     *
     * Critical operation with no CSRF protection and no authorization check
     *
     * @param id User ID to promote
     * @return Success message
     */
    @Operation(
        summary = "Promote to admin (CSRF + Broken Access Control)",
        description = "🔴 CSRF VULNERABILITY + BROKEN ACCESS CONTROL - Promotes user to admin via GET. " +
                     "No CSRF protection. No authorization check (any user can promote anyone). " +
                     "Attack: http://localhost:8080/api/v1/users/1/promote-admin"
    )
    @ApiResponse(responseCode = "200", description = "User promoted to admin")
    @GetMapping("/{id}/promote-admin")
    public ResponseEntity<String> promoteToAdminCSRF(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id) {
        // SEC-CSRF: Critical operation via GET with no CSRF protection
        // SEC-BAC: No authorization check - any user can promote anyone
        User user = userRepository.findById(id).get();
        user.setRole("ADMIN");
        userRepository.save(user);

        return ResponseEntity.ok("User " + id + " promoted to ADMIN successfully");
    }

    /**
     * SEC-CSRF: Transfer funds via GET - CSRF vulnerability
     *
     * Financial operation exposed via GET without protection
     *
     * @param fromId Source user ID
     * @param toId Destination user ID
     * @param amount Amount to transfer
     * @return Success message
     */
    @Operation(
        summary = "Transfer credits (CSRF VULNERABILITY)",
        description = "🔴 CSRF VULNERABILITY - Financial transaction via GET request. " +
                     "No CSRF protection. Can cause unauthorized fund transfers. " +
                     "Attack example: link with transfer parameters in URL"
    )
    @ApiResponse(responseCode = "200", description = "Transfer completed")
    @GetMapping("/transfer")
    public ResponseEntity<String> transferCreditsCSRF(
            @Parameter(description = "Source user ID", example = "1")
            @RequestParam Long fromId,
            @Parameter(description = "Destination user ID", example = "2")
            @RequestParam Long toId,
            @Parameter(description = "Amount to transfer", example = "100.00")
            @RequestParam String amount) {
        // SEC-CSRF: Financial transaction via GET
        // SEC-CSRF: No CSRF token validation
        // MNT: No validation of amount, balance, or business rules

        return ResponseEntity.ok("Transferred $" + amount + " from user " + fromId + " to user " + toId);
    }

    /**
     * SEC-BAC: Change any user's email - Broken Access Control
     *
     * No authorization check - any authenticated user can change any email
     *
     * @param userId User ID to modify
     * @param newEmail New email address
     * @return Success message
     */
    @Operation(
        summary = "Change user email (Broken Access Control)",
        description = "🔴 BROKEN ACCESS CONTROL - Any user can change ANY user's email address. " +
                     "No ownership verification. Attacker can lock out victims by changing their email."
    )
    @ApiResponse(responseCode = "200", description = "Email changed")
    @PostMapping("/{userId}/change-email")
    public ResponseEntity<String> changeEmailInsecure(
            @Parameter(description = "User ID to modify", example = "1")
            @PathVariable Long userId,
            @Parameter(description = "New email address", example = "attacker@evil.com")
            @RequestParam String newEmail) {
        // SEC-BAC: No authorization check
        // SEC-BAC: User A can modify User B's email
        User user = userRepository.findById(userId).get();
        user.setEmail(newEmail);
        userRepository.save(user);

        return ResponseEntity.ok("Email updated to: " + newEmail);
    }

    /**
     * SEC-BAC: Deactivate any user account - Broken Access Control
     *
     * No role check - regular users can deactivate admin accounts
     *
     * @param userId User ID to deactivate
     * @return Success message
     */
    @Operation(
        summary = "Deactivate user (Broken Access Control)",
        description = "🔴 BROKEN ACCESS CONTROL - Any user can deactivate ANY account including admins. " +
                     "No role-based authorization. Regular users can lock out administrators."
    )
    @ApiResponse(responseCode = "200", description = "User deactivated")
    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<String> deactivateUserInsecure(
            @Parameter(description = "User ID to deactivate", example = "1")
            @PathVariable Long userId) {
        // SEC-BAC: No authorization check
        // SEC-BAC: Regular users can deactivate admin accounts
        User user = userRepository.findById(userId).get();
        user.setActive(false);
        userRepository.save(user);

        return ResponseEntity.ok("User " + userId + " deactivated");
    }

    /**
     * SEC-BAC: View any user's sensitive data - Broken Access Control + IDOR
     *
     * Exposes SSN and credit card without authorization
     *
     * @param userId User ID
     * @return Sensitive user data
     */
    @Operation(
        summary = "View sensitive data (IDOR + Broken Access Control)",
        description = "🔴 IDOR + BROKEN ACCESS CONTROL - Exposes SSN and credit card numbers. " +
                     "Any user can view ANY user's sensitive financial data without authorization."
    )
    @ApiResponse(responseCode = "200", description = "Sensitive data exposed")
    @GetMapping("/{userId}/sensitive-data")
    public ResponseEntity<String> viewSensitiveDataInsecure(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long userId) {
        // SEC-BAC + IDOR: No authorization check
        // SEC: Exposing PII without protection
        User user = userRepository.findById(userId).get();

        String data = "SENSITIVE DATA FOR USER: " + user.getUsername() + "\n" +
                     "SSN: " + user.getSsn() + "\n" +
                     "Credit Card: " + user.getCreditCardNumber() + "\n" +
                     "Email: " + user.getEmail() + "\n" +
                     "Role: " + user.getRole();

        return ResponseEntity.ok(data);
    }
}

