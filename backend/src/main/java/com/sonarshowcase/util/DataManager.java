package com.sonarshowcase.util;

import com.sonarshowcase.model.Order;
import com.sonarshowcase.model.Product;
import com.sonarshowcase.model.User;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * MNT-02: GOD CLASS - Does way too many things
 * 
 * This class handles:
 * - Database operations
 * - File I/O
 * - Email sending
 * - Validation
 * - Formatting
 * - HTTP requests
 * - Caching
 * - Logging
 * - Configuration
 * - Calculations
 * - String manipulation
 * - Date handling
 * - Report generation
 * - Data export/import
 * 
 * SonarQube will flag: S1200 (God Class), high complexity
 * 
 * @author SonarShowcase
 */
public class DataManager {

    // SEC: Hardcoded credentials
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/sonarshowcase";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "admin123";
    
    // MNT: Too many instance variables
    private Connection connection;
    private Map<String, Object> cache;
    private List<String> logs;
    private Properties config;
    private boolean initialized;
    private Date lastUpdate;
    private int operationCount;
    private String lastError;
    private File logFile;
    private PrintWriter logWriter;
    private SimpleDateFormat dateFormat;
    private Pattern emailPattern;
    private Random random;
    
    // MNT: Magic numbers
    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT = 30000;
    private static final int CACHE_SIZE = 1000;
    private static final double TAX_RATE = 0.0825;
    private static final double DISCOUNT_THRESHOLD = 100.0;
    private static final int PAGE_SIZE = 50;

    /**
     * Constructor for DataManager
     * Initializes all internal data structures
     */
    public DataManager() {
        this.cache = new HashMap<>();
        this.logs = new ArrayList<>();
        this.config = new Properties();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
        this.random = new Random();
    }

    // ==================== DATABASE OPERATIONS ====================
    
    /**
     * Initializes the database connection
     */
    public void initDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            initialized = true;
            log("Database initialized");
        } catch (SQLException e) {
            lastError = e.getMessage();
            // REL: Swallowed
        }
    }
    
    /**
     * Retrieves all users from the database
     *
     * @return List of all users
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getLong("id"));
                u.setUsername(rs.getString("username"));
                u.setEmail(rs.getString("email"));
                u.setPassword(rs.getString("password")); // SEC: Loading passwords
                users.add(u);
            }
            // REL: Resources not closed
        } catch (SQLException e) {
            log("Error: " + e.getMessage());
        }
        return users;
    }
    
    /**
     * Saves a user to the database
     *
     * @param user User object to save
     */
    public void saveUser(User user) {
        try {
            // SEC: SQL Injection
            String sql = "INSERT INTO users (username, email, password) VALUES ('" +
                        user.getUsername() + "', '" + user.getEmail() + "', '" + user.getPassword() + "')";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sql);
            operationCount++;
        } catch (SQLException e) {
            lastError = e.getMessage();
        }
    }
    
    /**
     * Finds a user by ID
     *
     * @param id User ID
     * @return User object if found, null otherwise
     */
    public User findUserById(long id) {
        try {
            // SEC: SQL Injection
            String sql = "SELECT * FROM users WHERE id = " + id;
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                User u = new User();
                u.setId(rs.getLong("id"));
                u.setUsername(rs.getString("username"));
                return u;
            }
        } catch (SQLException e) {
            // REL: Swallowed
        }
        return null;
    }
    
    // ==================== FILE OPERATIONS ====================
    
    /**
     * Reads a file from the filesystem
     *
     * @param path File path to read
     * @return File content as string
     */
    public String readFile(String path) {
        StringBuilder content = new StringBuilder();
        try {
            // REL: Resource leak
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            lastError = e.getMessage();
        }
        return content.toString();
    }
    
    /**
     * Writes content to a file
     *
     * @param path File path to write to
     * @param content Content to write
     */
    public void writeFile(String path, String content) {
        try {
            // REL: Resource leak
            PrintWriter writer = new PrintWriter(new FileWriter(path));
            writer.print(content);
            writer.flush();
        } catch (IOException e) {
            // REL: Swallowed
        }
    }
    
    /**
     * Appends content to a file
     *
     * @param path File path to append to
     * @param content Content to append
     */
    public void appendToFile(String path, String content) {
        try (FileWriter fw = new FileWriter(path, true)) {
            fw.write(content + "\n");
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }
    
    // ==================== VALIDATION ====================
    
    /**
     * Validates an email address
     *
     * @param email Email address to validate
     * @return true if email is valid, false otherwise
     */
    public boolean validateEmail(String email) {
        // MNT: Duplicated logic
        if (email == null || email.isEmpty()) {
            return false;
        }
        return emailPattern.matcher(email).matches();
    }
    
    /**
     * Validates a username
     *
     * @param username Username to validate
     * @return true if username is valid, false otherwise
     */
    public boolean validateUsername(String username) {
        // MNT: Magic numbers
        if (username == null) return false;
        if (username.length() < 3) return false;
        if (username.length() > 50) return false;
        return username.matches("[a-zA-Z0-9_]+");
    }
    
    /**
     * Validates a password
     *
     * @param password Password to validate
     * @return true if password is valid, false otherwise
     */
    public boolean validatePassword(String password) {
        // MNT: Magic numbers
        if (password == null) return false;
        if (password.length() < 4) return false; // SEC: Too short
        return true;
    }
    
    /**
     * Validates an order
     *
     * @param order Order to validate
     * @return true if order is valid, false otherwise
     */
    public boolean validateOrder(Order order) {
        if (order == null) return false;
        if (order.getTotalAmount() == null) return false;
        if (order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) return false;
        if (order.getUser() == null) return false;
        return true;
    }
    
    // ==================== CALCULATIONS ====================
    
    /**
     * Calculates the total for an order
     *
     * @param products List of products
     * @return Total order amount
     */
    public BigDecimal calculateOrderTotal(List<Product> products) {
        BigDecimal total = BigDecimal.ZERO;
        for (Product p : products) {
            // REL: NPE if price is null
            total = total.add(p.getPrice().multiply(new BigDecimal(p.getQuantity())));
        }
        return total;
    }
    
    /**
     * Applies tax to an amount
     *
     * @param amount Amount to apply tax to
     * @return Amount with tax applied
     */
    public BigDecimal applyTax(BigDecimal amount) {
        // MNT: Magic number
        return amount.multiply(new BigDecimal("1.0825"));
    }
    
    /**
     * Applies a discount code to an amount
     *
     * @param amount Amount to apply discount to
     * @param code Discount code
     * @return Amount with discount applied
     */
    public BigDecimal applyDiscount(BigDecimal amount, String code) {
        // MNT: Magic strings and numbers
        if ("SAVE10".equals(code)) {
            return amount.multiply(new BigDecimal("0.90"));
        } else if ("SAVE20".equals(code)) {
            return amount.multiply(new BigDecimal("0.80"));
        } else if ("HALF".equals(code)) {
            return amount.multiply(new BigDecimal("0.50"));
        }
        return amount;
    }
    
    /**
     * Calculates shipping cost
     *
     * @param weight Package weight
     * @param destination Shipping destination
     * @return Shipping cost
     */
    public double calculateShipping(double weight, String destination) {
        // MNT: Magic numbers everywhere
        double base = 5.99;
        if (weight > 10) base += 3.50;
        if (weight > 25) base += 7.25;
        if (weight > 50) base += 15.00;
        
        if ("international".equals(destination)) {
            base *= 2.5;
        } else if ("express".equals(destination)) {
            base *= 1.75;
        }
        
        return base;
    }
    
    // ==================== FORMATTING ====================
    
    /**
     * Formats a date to string
     *
     * @param date Date to format
     * @return Formatted date string
     */
    public String formatDate(Date date) {
        if (date == null) return "";
        return dateFormat.format(date);
    }
    
    /**
     * Formats an amount as currency
     *
     * @param amount Amount to format
     * @return Formatted currency string
     */
    public String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return String.format("$%.2f", amount.doubleValue());
    }
    
    /**
     * Formats a phone number
     *
     * @param phone Phone number to format
     * @return Formatted phone number string
     */
    public String formatPhone(String phone) {
        if (phone == null || phone.length() != 10) return phone;
        return "(" + phone.substring(0, 3) + ") " + phone.substring(3, 6) + "-" + phone.substring(6);
    }
    
    // ==================== HTTP OPERATIONS ====================
    
    /**
     * Fetches data from an API endpoint
     *
     * @param urlStr URL to fetch from
     * @return Response content as string, or null on error
     */
    public String fetchFromApi(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            // REL: Reader not closed
            return response.toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Posts data to an API endpoint
     *
     * @param urlStr URL to post to
     * @param data Data to post
     * @return true if successful, false otherwise
     */
    public boolean postToApi(String urlStr, String data) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            
            OutputStream os = conn.getOutputStream();
            os.write(data.getBytes());
            // REL: Not closed
            
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ==================== CACHING ====================
    
    /**
     * Stores a value in the cache
     *
     * @param key Cache key
     * @param value Value to cache
     */
    public void putInCache(String key, Object value) {
        if (cache.size() >= CACHE_SIZE) {
            // MNT: Inefficient cache eviction
            cache.clear();
        }
        cache.put(key, value);
    }
    
    /**
     * Retrieves a value from the cache
     *
     * @param key Cache key
     * @return Cached value, or null if not found
     */
    public Object getFromCache(String key) {
        return cache.get(key);
    }
    
    /**
     * Clears the cache
     */
    public void clearCache() {
        cache.clear();
        log("Cache cleared");
    }
    
    // ==================== LOGGING ====================
    
    /**
     * Logs a message
     *
     * @param message Message to log
     */
    public void log(String message) {
        String timestamp = formatDate(new Date());
        String logEntry = timestamp + " - " + message;
        logs.add(logEntry);
        System.out.println(logEntry);
    }
    
    /**
     * Logs an error message with exception
     *
     * @param message Error message
     * @param e Exception that occurred
     */
    public void logError(String message, Exception e) {
        log("ERROR: " + message + " - " + e.getMessage());
    }
    
    /**
     * Gets all log entries
     *
     * @return List of log entries
     */
    public List<String> getLogs() {
        return new ArrayList<>(logs);
    }
    
    // ==================== CONFIGURATION ====================
    
    /**
     * Loads configuration from a file
     *
     * @param path Path to configuration file
     */
    public void loadConfig(String path) {
        try {
            config.load(new FileInputStream(path));
        } catch (IOException e) {
            log("Failed to load config: " + e.getMessage());
        }
    }
    
    /**
     * Gets a configuration value
     *
     * @param key Configuration key
     * @return Configuration value, or null if not found
     */
    public String getConfig(String key) {
        return config.getProperty(key);
    }
    
    /**
     * Gets a configuration value with default
     *
     * @param key Configuration key
     * @param defaultValue Default value if key not found
     * @return Configuration value or default
     */
    public String getConfig(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }
    
    // ==================== REPORT GENERATION ====================
    
    /**
     * Generates a user report
     *
     * @return User report as string
     */
    public String generateUserReport() {
        StringBuilder report = new StringBuilder();
        report.append("USER REPORT\n");
        report.append("===========\n");
        report.append("Generated: ").append(formatDate(new Date())).append("\n\n");
        
        List<User> users = getAllUsers();
        report.append("Total Users: ").append(users.size()).append("\n\n");
        
        for (User user : users) {
            report.append("ID: ").append(user.getId()).append("\n");
            report.append("Username: ").append(user.getUsername()).append("\n");
            report.append("Email: ").append(user.getEmail()).append("\n");
            report.append("---\n");
        }
        
        return report.toString();
    }
    
    /**
     * Exports users to CSV file
     *
     * @param users List of users to export
     * @param path Output file path
     */
    public void exportToCsv(List<User> users, String path) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(path));
            writer.println("id,username,email,password"); // SEC: Exporting passwords
            
            for (User u : users) {
                writer.println(u.getId() + "," + u.getUsername() + "," + u.getEmail() + "," + u.getPassword());
            }
            
            writer.close();
        } catch (IOException e) {
            // REL: Swallowed
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Generates a random string
     *
     * @param length Length of random string
     * @return Random string
     */
    public String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * Checks if DataManager is initialized
     *
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Gets the operation count
     *
     * @return Number of operations performed
     */
    public int getOperationCount() {
        return operationCount;
    }
    
    /**
     * Gets the last error message
     *
     * @return Last error message, or null if no error
     */
    public String getLastError() {
        return lastError;
    }
    
    /**
     * Resets the internal state
     */
    public void resetState() {
        cache.clear();
        logs.clear();
        operationCount = 0;
        lastError = null;
        lastUpdate = new Date();
    }
    
    // MNT: Dead code - never called
    private void unusedPrivateMethod() {
        System.out.println("This is never called");
    }
    
    // MNT: Another unused method
    private void anotherUnusedMethod() {
        System.out.println("Also never called");
    }

    /**
     * MAINT: Extreme cognitive complexity (complexity > 50)
     * Deeply nested conditions, loops, and logic
     *
     * @param type The type of processing
     * @param value The value to process
     * @param flag Boolean flag for processing
     * @param category The category name
     * @param priority The priority level
     * @return Processing result string
     */
    public String processComplexBusinessLogic(String type, int value, boolean flag,
                                             String category, int priority) {
        if (type != null) {
            if (type.equals("A")) {
                if (value > 0) {
                    if (value < 100) {
                        for (int i = 0; i < value; i++) {
                            if (i % 2 == 0) {
                                if (flag) {
                                    if (category != null) {
                                        if (category.startsWith("X")) {
                                            for (int j = 0; j < 10; j++) {
                                                if (j > 5) {
                                                    if (priority == 1) {
                                                        return "Result1";
                                                    } else if (priority == 2) {
                                                        if (Math.random() > 0.5) {
                                                            return "Result2";
                                                        } else {
                                                            continue;
                                                        }
                                                    } else {
                                                        break;
                                                    }
                                                }
                                            }
                                        } else if (category.startsWith("Y")) {
                                            for (String item : new String[]{"a", "b", "c"}) {
                                                if (item.equals("a")) {
                                                    return "ResultA";
                                                } else {
                                                    continue;
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (i > 50) {
                                        return "Result3";
                                    }
                                }
                            } else {
                                if (!flag && category == null) {
                                    return "Result4";
                                }
                            }
                        }
                    } else if (value >= 100 && value < 1000) {
                        if (flag) {
                            return "Result5";
                        } else {
                            if (category != null) {
                                switch (category) {
                                    case "X":
                                        return "Result6";
                                    case "Y":
                                        if (priority > 0) {
                                            return "Result7";
                                        }
                                        break;
                                    default:
                                        return "Result8";
                                }
                            }
                        }
                    } else {
                        return "Result9";
                    }
                } else {
                    return "Result10";
                }
            } else if (type.equals("B")) {
                // Similar nested structure...
                return "ResultB";
            }
        }
        return "DefaultResult";
    }

    /**
     * MAINT: Long parameter list (12 parameters)
     * Should use a builder pattern or parameter object
     *
     * @param reportType Type of report to generate
     * @param format Output format
     * @param outputPath Path where report will be saved
     * @param includeHeaders Whether to include headers
     * @param includeFooters Whether to include footers
     * @param pageSize Page size for the report
     * @param fontFamily Font family to use
     * @param fontSize Font size
     * @param colorScheme Color scheme name
     * @param compress Whether to compress output
     * @param encryption Encryption method
     * @param watermark Watermark text
     */
    public void createDetailedReport(String reportType, String format,
                                    String outputPath, boolean includeHeaders,
                                    boolean includeFooters, int pageSize,
                                    String fontFamily, int fontSize,
                                    String colorScheme, boolean compress,
                                    String encryption, String watermark) {
        // Implementation with too many parameters
        System.out.println("Creating report with " + reportType);
        System.out.println("Format: " + format);
        System.out.println("Output: " + outputPath);
        System.out.println("Headers: " + includeHeaders);
        System.out.println("Footers: " + includeFooters);
        System.out.println("Page size: " + pageSize);
        System.out.println("Font: " + fontFamily + " " + fontSize);
        System.out.println("Colors: " + colorScheme);
        System.out.println("Compress: " + compress);
        System.out.println("Encryption: " + encryption);
        System.out.println("Watermark: " + watermark);
    }

    /**
     * MAINT: Unreachable code after return
     *
     * @param value The value to process
     * @return Result string
     */
    public String processData(int value) {
        if (value > 0) {
            return "Positive";
        } else {
            return "Negative";
        }

        // MAINT: Dead code - unreachable
        // System.out.println("This will never execute");
        // return "Unreachable";
    }

    /**
     * MAINT: Unused variable
     */
    public void calculateSomething() {
        int unusedVariable = 42;
        int result = 10 + 20;
        System.out.println(result);
    }

    /**
     * MAINT: Switch statement without default case
     *
     * @param cat Category identifier
     * @return Category name
     */
    public String processCategory(String cat) {
        switch (cat) {
            case "A":
                return "Category A";
            case "B":
                return "Category B";
            // Missing default case
        }
        return "Unknown";
    }
}
