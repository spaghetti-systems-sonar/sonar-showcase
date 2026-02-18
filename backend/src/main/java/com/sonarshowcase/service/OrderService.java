package com.sonarshowcase.service;

import com.sonarshowcase.model.Order;
import com.sonarshowcase.model.User;
import com.sonarshowcase.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Order service with tight coupling and reliability issues.
 * 
 * MNT: Tight coupling with UserService
 * REL-01: Null pointer dereference risks
 * 
 * @author SonarShowcase
 */
@Service
public class OrderService {
    
    /**
     * Default constructor for OrderService.
     */
    public OrderService() {
    }

    // MNT: Field injection instead of constructor injection
    @Autowired
    private OrderRepository orderRepository;
    
    // MNT: Lazy injection - architectural smell, these services are too coupled
    @Autowired
    @org.springframework.context.annotation.Lazy
    private UserService userService;
    
    // MNT: Part of 6-level cycle: ... -> ActivityLogService -> OrderService -> PaymentService -> ...
    @Autowired
    @org.springframework.context.annotation.Lazy
    private PaymentService paymentService;
    
    /**
     * Gets all orders
     *
     * @return List of all orders
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    /**
     * REL-01: NPE - using .get() on Optional without check
     *
     * @param id Order ID
     * @return Order if found
     */
    public Order getOrderById(Long id) {
        // REL: This will throw NoSuchElementException if not found
        return orderRepository.findById(id).get();
    }
    
    /**
     * Gets all orders for a user
     *
     * @param userId User ID
     * @return List of orders for the user
     */
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
    
    /**
     * Creates a new order
     *
     * @param order Order to create
     * @return Created order
     */
    public Order createOrder(Order order) {
        // REL: NPE if order.getUser() is null
        Long userId = order.getUser().getId();
        
        // MNT: Circular call to userService
        User user = userService.getUserById(userId);
        
        // REL: NPE if user is null
        order.setUser(user);
        order.setOrderNumber(generateOrderNumber());
        order.setOrderDate(new Date());
        order.setStatus("PENDING"); // MNT: Magic string
        
        return orderRepository.save(order);
    }
    
    /**
     * Deletes all orders for a user
     *
     * @param userId User ID
     */
    public void deleteOrdersByUser(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        // REL: NPE if orders is null (shouldn't happen but not checked)
        for (Order order : orders) {
            orderRepository.delete(order);
        }
    }
    
    /**
     * Gets the count of orders for a user
     *
     * @param userId User ID
     * @return Number of orders
     */
    public int getOrderCountByUser(Long userId) {
        return orderRepository.findByUserId(userId).size();
    }
    
    /**
     * Calculate order total with magic numbers
     *
     * @param order Order to calculate total for
     * @return Calculated total amount
     */
    public BigDecimal calculateTotal(Order order) {
        BigDecimal subtotal = order.getTotalAmount();
        
        // MNT: Magic numbers everywhere
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.0825")); // 8.25% tax
        BigDecimal shipping = new BigDecimal("5.99");
        
        // MNT: More magic numbers
        if (subtotal.compareTo(new BigDecimal("50")) > 0) {
            shipping = BigDecimal.ZERO; // Free shipping over $50
        }
        
        // MNT: Another magic number
        if (subtotal.compareTo(new BigDecimal("100")) > 0) {
            // 10% discount for orders over $100
            subtotal = subtotal.multiply(new BigDecimal("0.9"));
        }
        
        return subtotal.add(tax).add(shipping);
    }
    
    private String generateOrderNumber() {
        // MNT: Overly complex for simple task
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Process order - too many responsibilities
     *
     * @param orderId Order ID to process
     * @return Processed order
     */
    public Order processOrder(Long orderId) {
        Order order = getOrderById(orderId);
        
        // REL: NPE chain
        String email = order.getUser().getEmail();
        
        // MNT: Business logic that should be elsewhere
        order.setStatus("PROCESSING");
        
        // MNT: Debug print
        System.out.println("Processing order for: " + email);
        
        return orderRepository.save(order);
    }
    
    /**
     * MNT: Part of 6-level cycle - OrderService -> PaymentService -> ...
     * Completes the 6-level cycle back to PaymentService
     *
     * @param order Order to process payment for
     * @param cardNumber Credit card number
     * @return true if payment processed
     */
    public boolean processOrderPayment(Order order, String cardNumber) {
        // MNT: Using PaymentService completes 6-level cycle: OrderService -> PaymentService -> EmailService -> ...
        return paymentService.processPayment(order, cardNumber, "123");
    }

    /**
     * Updates an existing order
     * No ownership validation - used by IDOR vulnerable endpoints
     *
     * @param order Order to update
     * @return Updated order
     */
    public Order updateOrder(Order order) {
        // MNT: No validation or ownership check
        return orderRepository.save(order);
    }
}

