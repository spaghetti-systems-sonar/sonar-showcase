package com.sonarshowcase.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Email service with swallowed exceptions.
 * 
 * REL-03: Swallowed exceptions - empty catch blocks
 * MNT: Circular dependency with PaymentService (architecture violation)
 * 
 * @author SonarShowcase
 */
@Service
public class EmailService {
    
    /**
     * Default constructor for EmailService.
     */
    public EmailService() {
    }

    // MNT: Circular dependency - EmailService depends on PaymentService
    // PaymentService also depends on EmailService (architecture violation)
    @Autowired
    @org.springframework.context.annotation.Lazy
    private PaymentService paymentService;
    
    // MNT: Part of 6-level cycle: PaymentService -> EmailService -> CategoryService -> ...
    @Autowired
    @org.springframework.context.annotation.Lazy
    private CategoryService categoryService;

    // SEC: Hardcoded email credentials
    private static final String EMAIL_USERNAME = "sonarshowcase@gmail.com";
    private static final String EMAIL_PASSWORD = "app_password_123456";
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    
    /**
     * REL-03: Swallowed exception - exception is logged but not properly handled
     *
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body content
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            // SEC: Logging sensitive information
            System.out.println("Sending email to: " + to);
            System.out.println("Using credentials: " + EMAIL_USERNAME + " / " + EMAIL_PASSWORD);
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + body);
            
            // MNT: Simulated email sending
            if (to == null || to.isEmpty()) {
                throw new RuntimeException("Invalid email address");
            }
            
            // MNT: Magic number for simulated delay
            Thread.sleep(100);
            
            System.out.println("Email sent successfully to: " + to);
            
        } catch (Exception e) {
            // REL-03: Swallowed exception - only printing, not properly handling
            System.out.println("Email failed");
            // This is a critical bug - email failures are silently ignored
        }
    }
    
    /**
     * REL: Another swallowed exception
     *
     * @param email Recipient email address
     * @param username Username for welcome message
     */
    public void sendWelcomeEmail(String email, String username) {
        try {
            String subject = "Welcome to SonarShowcase!";
            String body = "Hello " + username + ", welcome to our platform!";
            sendEmail(email, subject, body);
        } catch (Exception e) {
            // REL: Swallowed - user never knows email failed
        }
    }
    
    /**
     * REL: Exception swallowed with just a print
     *
     * @param email Recipient email address
     * @param token Password reset token
     */
    public void sendPasswordResetEmail(String email, String token) {
        try {
            String subject = "Password Reset";
            String body = "Your reset token is: " + token;
            sendEmail(email, subject, body);
        } catch (Exception e) {
            // REL: Only printing, not properly handling
            System.out.println("Failed to send email");
        }
    }
    
    /**
     * REL: Catching generic Exception
     *
     * @param email Recipient email address
     * @param orderNumber Order number for confirmation
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendOrderConfirmation(String email, String orderNumber) {
        try {
            String subject = "Order Confirmation: " + orderNumber;
            String body = "Your order " + orderNumber + " has been confirmed.";
            sendEmail(email, subject, body);
            return true;
        } catch (Exception e) {
            // REL: Catching generic Exception instead of specific ones
            // REL: Returning false hides the actual error
            return false;
        }
    }
    
    /**
     * REL: Resource leak - stream not closed
     *
     * @param templatePath Path to email template file
     * @return Template content as string, or empty string on error
     */
    public String readEmailTemplate(String templatePath) {
        try (FileInputStream fis = new FileInputStream(new File(templatePath))) {
            byte[] data = new byte[fis.available()];
            fis.read(data);
            return new String(data);
        } catch (IOException e) {
            return "";
        }
    }
    
    /**
     * MNT: Circular dependency usage - EmailService calling PaymentService
     * This creates a circular dependency: PaymentService -> EmailService -> PaymentService
     *
     * @param email Recipient email address
     * @param amount Payment amount to verify
     * @return true if payment verification email sent successfully
     */
    public boolean sendPaymentVerificationEmail(String email, String amount) {
        try {
            // MNT: Circular dependency - calling PaymentService from EmailService
            // This creates an architectural violation
            String subject = "Payment Verification Required";
            String body = "Please verify your payment of " + amount;
            sendEmail(email, subject, body);
            
            // MNT: Unnecessary call to payment service for validation
            // This tightens the circular dependency
            return true;
        } catch (Exception e) {
            // REL: Swallowed exception
            return false;
        }
    }
    
    /**
     * MNT: Part of 6-level cycle - EmailService -> CategoryService -> ...
     * 
     * @param email Recipient email
     * @param categoryId Category to include in email
     */
    public void sendCategoryUpdateEmail(String email, String categoryId) {
        // MNT: Using CategoryService creates dependency in 6-level cycle
        int depth = categoryService.calculateDepth(categoryId);
        String subject = "Category Update";
        String body = "Category " + categoryId + " has depth " + depth;
        sendEmail(email, subject, body);
    }
}

