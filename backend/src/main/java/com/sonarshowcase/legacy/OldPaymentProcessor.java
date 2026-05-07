package com.sonarshowcase.legacy;

import java.math.BigDecimal;
// import java.util.Date;
// import java.util.HashMap;
// import java.util.Map;
// import java.net.HttpURLConnection;
// import java.net.URL;

/**
 * Old payment processor - kept for "reference".
 * 
 * MNT-11: Commented-out code blocks
 * 
 * @author SonarShowcase
 */
public class OldPaymentProcessor {
    
    /**
     * Default constructor for OldPaymentProcessor.
     */
    public OldPaymentProcessor() {
    }

    // SEC: Old API keys (commented but still a security smell)
    // private static final String OLD_API_KEY = "sk_old_key_12345";
    // private static final String OLD_SECRET = "secret_old_67890";
    
    /*
     * MNT-11: Large block of commented-out code - S125
     * 
     * public boolean processPayment(String cardNumber, BigDecimal amount) {
     *     try {
     *         URL url = new URL("https://old-api.payment.com/v1/charge");
     *         HttpURLConnection conn = (HttpURLConnection) url.openConnection();
     *         conn.setRequestMethod("POST");
     *         conn.setDoOutput(true);
     *         
     *         Map<String, String> params = new HashMap<>();
     *         params.put("card", cardNumber);
     *         params.put("amount", amount.toString());
     *         params.put("api_key", OLD_API_KEY);
     *         
     *         // Write request
     *         String requestBody = buildRequestBody(params);
     *         conn.getOutputStream().write(requestBody.getBytes());
     *         
     *         // Read response
     *         int responseCode = conn.getResponseCode();
     *         if (responseCode == 200) {
     *             return true;
     *         }
     *         
     *         return false;
     *     } catch (Exception e) {
     *         e.printStackTrace();
     *         return false;
     *     }
     * }
     */
    
    /**
     * Processes a payment using new method
     *
     * @param amount Payment amount
     * @return true if payment processed successfully
     */
    public boolean processPaymentNew(BigDecimal amount) {
        // TODO: Implement new payment processing
        // FIXME: This is a placeholder
        System.out.println("Processing payment: " + amount);
        return true;
    }
    
    /*
    // MNT: Another large commented block
    private String buildRequestBody(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }
    
    private boolean validateCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 13) {
            return false;
        }
        // Luhn algorithm
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
    
    private void logTransaction(String cardNumber, BigDecimal amount, boolean success) {
        Date now = new Date();
        String maskedCard = "****" + cardNumber.substring(cardNumber.length() - 4);
        System.out.println("[" + now + "] Transaction: " + maskedCard + ", Amount: " + amount + ", Success: " + success);
    }
    */
    
    // MNT: Commented-out method
    // public void refund(String transactionId) {
    //     // Old refund logic
    //     System.out.println("Refunding: " + transactionId);
    // }
    
    // MNT: More commented code
    // private static final int RETRY_COUNT = 3;
    // private static final int TIMEOUT_MS = 30000;
    
    // MNT: Unused field kept for "future use"
    @SuppressWarnings("unused")
    private String legacyEndpoint = "https://old-api.example.com";
    
    /*
     * OLD IMPLEMENTATION - DO NOT DELETE (keeping for reference)
     * 
     * public class PaymentResult {
     *     private boolean success;
     *     private String transactionId;
     *     private String errorMessage;
     *     private Date timestamp;
     *     
     *     // Getters and setters...
     * }
     * 
     * public PaymentResult charge(String card, BigDecimal amount, String currency) {
     *     PaymentResult result = new PaymentResult();
     *     try {
     *         // Old charging logic
     *         result.setSuccess(true);
     *         result.setTransactionId(UUID.randomUUID().toString());
     *         result.setTimestamp(new Date());
     *     } catch (Exception e) {
     *         result.setSuccess(false);
     *         result.setErrorMessage(e.getMessage());
     *     }
     *     return result;
     * }
     */
}

