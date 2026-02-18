package com.sonarshowcase.controller;

import com.sonarshowcase.model.Order;
import com.sonarshowcase.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Order controller.
 * 
 * @author SonarShowcase
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management API endpoints")
public class OrderController {
    
    /**
     * Default constructor for OrderController.
     */
    public OrderController() {
    }

    @Autowired
    private OrderService orderService;
    
    /**
     * Gets all orders
     *
     * @return ResponseEntity containing list of all orders
     */
    @Operation(summary = "Get all orders", description = "Returns all orders in the system")
    @ApiResponse(responseCode = "200", description = "List of all orders")
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
    
    /**
     * Gets an order by ID
     *
     * @param id Order ID
     * @return ResponseEntity containing the order
     */
    @Operation(summary = "Get order by ID", description = "Returns an order by ID. ⚠️ BUG: Throws exception if order not found (NPE risk)")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "500", description = "Order not found (throws exception)")
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(
            @Parameter(description = "Order ID", example = "1")
            @PathVariable Long id) {
        // REL: NPE risk from service
        return ResponseEntity.ok(orderService.getOrderById(id));
    }
    
    /**
     * Gets all orders for a specific user
     *
     * @param userId User ID
     * @return ResponseEntity containing list of user's orders
     */
    @Operation(summary = "Get orders by user", description = "Returns all orders for a specific user")
    @ApiResponse(responseCode = "200", description = "List of user's orders")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUser(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }
    
    /**
     * Creates a new order
     *
     * @param order Order data to create
     * @return ResponseEntity containing the created order
     */
    @Operation(
        summary = "Create new order", 
        description = "Creates a new order. ⚠️ MNT: No validation of order data",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Order data", 
            required = true, 
            content = @Content(schema = @Schema(implementation = Order.class))
        )
    )
    @ApiResponse(responseCode = "200", description = "Order created")
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        // MNT: No validation
        return ResponseEntity.ok(orderService.createOrder(order));
    }
    
    /**
     * Apply discount - uses magic numbers
     *
     * @param id Order ID
     * @param code Discount code (SUMMER2023, VIP, or EMPLOYEE)
     * @return ResponseEntity containing order with discount applied
     */
    @Operation(
        summary = "Apply discount code", 
        description = "Applies a discount code to an order. Valid codes: SUMMER2023 (15%), VIP (25%), EMPLOYEE (50%). " +
                     "⚠️ BUG: Changes are not persisted to database (intentional maintainability issue)"
    )
    @ApiResponse(responseCode = "200", description = "Order with discount applied")
    @PostMapping("/{id}/discount")
    public ResponseEntity<Order> applyDiscount(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(description = "Discount code (SUMMER2023, VIP, or EMPLOYEE)", example = "SUMMER2023")
            @RequestParam String code) {
        
        Order order = orderService.getOrderById(id);
        
        // MNT: Magic strings and numbers
        if ("SUMMER2023".equals(code)) {
            BigDecimal discount = order.getTotalAmount().multiply(new BigDecimal("0.15"));
            order.setTotalAmount(order.getTotalAmount().subtract(discount));
        } else if ("VIP".equals(code)) {
            BigDecimal discount = order.getTotalAmount().multiply(new BigDecimal("0.25"));
            order.setTotalAmount(order.getTotalAmount().subtract(discount));
        } else if ("EMPLOYEE".equals(code)) {
            // MNT: Hardcoded 50% discount
            order.setTotalAmount(order.getTotalAmount().divide(new BigDecimal("2")));
        }
        
        // MNT: No persistence of changes
        return ResponseEntity.ok(order);
    }

    /**
     * Cancel order - IDOR vulnerability
     *
     * SEC-IDOR: No ownership verification - any user can cancel any order (S6417)
     * This demonstrates Insecure Direct Object Reference vulnerability where
     * a user can manipulate the orderId parameter to cancel orders belonging to other users.
     *
     * @param orderId Order ID to cancel
     * @return ResponseEntity with success message
     */
    @Operation(
        summary = "Cancel order (IDOR vulnerability)",
        description = "🔴 SECURITY VULNERABILITY: Insecure Direct Object Reference (IDOR). " +
                     "Any user can cancel ANY order by manipulating the orderId parameter. " +
                     "No ownership check is performed. Example: User A can cancel User B's orders."
    )
    @ApiResponse(responseCode = "200", description = "Order cancelled successfully")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrderInsecure(
            @Parameter(description = "Order ID", example = "1")
            @PathVariable Long orderId) {
        // SEC-IDOR: No check if current user owns this order
        // Rule S6417: Insecure Direct Object Reference
        Order order = orderService.getOrderById(orderId);
        order.setStatus("CANCELLED");
        orderService.updateOrder(order);

        return ResponseEntity.ok("Order " + orderId + " cancelled successfully");
    }

    /**
     * View order invoice - IDOR vulnerability
     *
     * SEC-IDOR: No authorization check - any user can view any invoice (S6417)
     *
     * @param orderId Order ID
     * @return ResponseEntity with invoice details
     */
    @Operation(
        summary = "View order invoice (IDOR vulnerability)",
        description = "🔴 SECURITY VULNERABILITY: IDOR. Any user can view ANY order's invoice. " +
                     "Exposes sensitive financial information without authorization checks."
    )
    @ApiResponse(responseCode = "200", description = "Invoice details")
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<String> viewInvoiceInsecure(
            @Parameter(description = "Order ID", example = "1")
            @PathVariable Long orderId) {
        // SEC-IDOR: No ownership verification
        Order order = orderService.getOrderById(orderId);

        // SEC: Exposing sensitive financial details
        String invoice = "INVOICE\n" +
                        "Order ID: " + order.getId() + "\n" +
                        "Order Number: " + order.getOrderNumber() + "\n" +
                        "User ID: " + order.getUser().getId() + "\n" +
                        "User Email: " + order.getUser().getEmail() + "\n" +
                        "Total Amount: $" + order.getTotalAmount() + "\n" +
                        "Status: " + order.getStatus() + "\n" +
                        "Shipping Address: " + order.getShippingAddress();

        return ResponseEntity.ok(invoice);
    }

    /**
     * Update order shipping address - IDOR vulnerability
     *
     * SEC-IDOR: Any user can modify any order's shipping address (S6417)
     *
     * @param orderId Order ID
     * @param newAddress New shipping address
     * @return ResponseEntity with updated order
     */
    @Operation(
        summary = "Update shipping address (IDOR vulnerability)",
        description = "🔴 SECURITY VULNERABILITY: IDOR. Any user can modify ANY order's shipping address. " +
                     "This could be used to redirect shipments to attacker-controlled addresses."
    )
    @ApiResponse(responseCode = "200", description = "Shipping address updated")
    @PutMapping("/{orderId}/shipping-address")
    public ResponseEntity<Order> updateShippingAddressInsecure(
            @Parameter(description = "Order ID", example = "1")
            @PathVariable Long orderId,
            @Parameter(description = "New shipping address", example = "123 Hacker Street")
            @RequestParam String newAddress) {
        // SEC-IDOR: No authorization check - attacker can redirect shipments
        Order order = orderService.getOrderById(orderId);
        order.setShippingAddress(newAddress);
        orderService.updateOrder(order);

        return ResponseEntity.ok(order);
    }
}

