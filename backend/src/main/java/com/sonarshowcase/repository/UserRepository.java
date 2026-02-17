package com.sonarshowcase.repository;

import com.sonarshowcase.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

/**
 * User repository with SQL injection vulnerabilities
 * 
 * SEC-01: SQL Injection via string concatenation
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by username
     * 
     * @param username The username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Finds a user by email
     * 
     * @param email The email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Finds all users with the specified role
     * 
     * @param role The role to search for
     * @return List of users with the specified role
     */
    List<User> findByRole(String role);
    
    /**
     * Finds active users by role
     * MNT: Overly complex query
     * 
     * @param role The role to search for
     * @return List of active users with the specified role, ordered by creation date descending
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND u.role = ?1 ORDER BY u.createdAt DESC")
    List<User> findActiveUsersByRole(String role);
}

/**
 * Custom repository implementation with SQL injection
 */
@Repository
class UserRepositoryCustomImpl {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * SEC-01: SQL Injection vulnerability
     * User input is directly concatenated into SQL query
     */
    public List<User> findUsersBySearch(String searchTerm) {
        // SEC: SQL Injection - S3649
        String sql = "SELECT * FROM users WHERE username LIKE '%" + searchTerm + "%' " +
                     "OR email LIKE '%" + searchTerm + "%'";
        
        return entityManager.createNativeQuery(sql, User.class).getResultList();
    }
    
    /**
     * SEC: Another SQL injection vulnerability
     */
    public User authenticateUser(String username, String password) {
        // SEC: SQL Injection in authentication - critical vulnerability
        String sql = "SELECT * FROM users WHERE username = '" + username + 
                     "' AND password = '" + password + "'";
        
        try {
            return (User) entityManager.createNativeQuery(sql, User.class).getSingleResult();
        } catch (Exception e) {
            return null; // REL: Swallowing exception
        }
    }
    
    /**
     * SEC: SQL injection with ORDER BY
     */
    public List<User> findUsersOrderedBy(String column) {
        // SEC: SQL Injection via ORDER BY clause
        String sql = "SELECT * FROM users ORDER BY " + column;
        return entityManager.createNativeQuery(sql, User.class).getResultList();
    }
    
    /**
     * SEC: SQL injection in DELETE statement
     */
    public void deleteUsersByRole(String role) {
        // SEC: SQL Injection in DELETE - very dangerous
        String sql = "DELETE FROM users WHERE role = '" + role + "'";
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    /**
     * SEC: SQL Injection in INSERT statement
     */
    public void insertUserUnsafe(String username, String email) {
        // SEC: SQL Injection in INSERT - can inject malicious data
        String sql = "INSERT INTO users (username, email) VALUES ('" + username + "', '" + email + "')";
        // Attack: username = admin', 'admin@example.com'); DROP TABLE users;--
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    /**
     * SEC: SQL Injection in UPDATE statement
     */
    public void updateUserEmailUnsafe(Long userId, String email) {
        // SEC: SQL Injection in UPDATE
        String sql = "UPDATE users SET email = '" + email + "' WHERE id = " + userId;
        // Attack: email = admin@example.com', role='ADMIN' WHERE '1'='1
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    /**
     * SEC: Second-order SQL Injection
     * User input stored in DB is later used in unsafe query
     */
    public List<User> findUsersByStoredPreference(Long userId) {
        // First, get user's stored search preference (could be malicious)
        User user = entityManager.find(User.class, userId);
        if (user == null || user.getUsername() == null) {
            return List.of();
        }
        String searchPref = user.getUsername(); // Could contain: ' OR '1'='1

        // Second, use it in unsafe query (second-order injection)
        String sql = "SELECT * FROM users WHERE username = '" + searchPref + "'";
        try {
            return entityManager.createNativeQuery(sql, User.class).getResultList();
        } catch (Exception e) {
            return List.of(); // REL: Swallowing exception
        }
    }

    /**
     * SEC: SQL Injection in LIMIT clause
     */
    public List<User> findUsersWithLimitUnsafe(String limitValue) {
        // SEC: SQL Injection via LIMIT
        String sql = "SELECT * FROM users LIMIT " + limitValue;
        // Attack: limitValue = "1; DROP TABLE users;--"
        try {
            return entityManager.createNativeQuery(sql, User.class).getResultList();
        } catch (Exception e) {
            return List.of(); // REL: Swallowing exception
        }
    }
}

