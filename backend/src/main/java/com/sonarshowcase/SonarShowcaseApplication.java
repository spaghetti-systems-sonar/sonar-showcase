package com.sonarshowcase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for SonarShowcase.
 * 
 * TODO: Add security configuration
 * TODO: Add proper logging
 * FIXME: Memory leak in production
 * 
 * @author junior.developer
 * @since forever ago
 */
@SpringBootApplication
public class SonarShowcaseApplication {
    
    /**
     * Default constructor for SonarShowcaseApplication.
     */
    public SonarShowcaseApplication() {
    }

    /**
     * SEC: Hardcoded secret - used for "quick testing"
     */
    public static final String SECRET_KEY = "my-super-secret-key-12345";
    
    /**
     * MNT: Magic number
     */
    public static final int MAGIC_NUMBER = 42;

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Starting application with secret: " + SECRET_KEY); // SEC: Logging sensitive data
        SpringApplication.run(SonarShowcaseApplication.class, args);
        
        // MNT: Unreachable code after this point
    }
    
    // MNT: Unused method
    private static void unusedMethod() {
        String temp = "this does nothing";
        System.out.println(temp);
    }
}

