package com.rama.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository repository;
    private final UserService userService;
    private final EmailService emailService;

    public OrderController(OrderRepository repository, UserService userService, EmailService emailService) {
        this.repository = repository;
        this.userService = userService;
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody Order order,
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        
        String effectiveEmail = "guest@rama.local".equals(userEmail) ? order.getUserEmail() : userEmail;

        if (effectiveEmail == null || effectiveEmail.trim().isEmpty() || "guest".equals(effectiveEmail)) {
            return ResponseEntity.badRequest().body("Email is mandatory for all users.");
        }

        if (repository.existsByUserEmail(effectiveEmail)) {
            return ResponseEntity.badRequest().body("Order is already placed for " + effectiveEmail + ". Please contact mangoes.bern@gmail.com.");
        }

        String validationError = validateOrder(order, effectiveEmail);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(validationError);
        }

        order.setUserEmail(effectiveEmail);
        Order savedOrder = repository.save(order);
        try {
            emailService.sendOrderConfirmation(savedOrder);
        } catch (Exception e) {
            System.err.println("Failed to send order confirmation email: " + e.getMessage());
        }
        return ResponseEntity.ok(savedOrder);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrder(
            @PathVariable String id,
            @RequestBody Order orderDetails,
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        
        Order order = repository.findById(id).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        Role role = userService.getRoleByEmail(userEmail);
        if (role != Role.ADMIN && !order.getUserEmail().equals(userEmail)) {
            return ResponseEntity.status(403).build();
        }

        String validationError = validateOrder(orderDetails, userEmail);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(validationError);
        }

        order.setName(orderDetails.getName());
        order.setAddress(orderDetails.getAddress());
        order.setPhone(orderDetails.getPhone());
        order.setQuantity(orderDetails.getQuantity());
        order.setPickupLocation(orderDetails.getPickupLocation());

        Order savedOrder = repository.save(order);
        try {
            emailService.sendOrderUpdateNotification(savedOrder);
        } catch (Exception e) {
            System.err.println("Failed to send order update email: " + e.getMessage());
        }
        return ResponseEntity.ok(savedOrder);
    }

    private String validateOrder(Order order, String userEmail) {
        Role role = userService.getRoleByEmail(userEmail);
        if (role == Role.ADMIN) {
            return null; // No restriction for admin
        }

        boolean isGoogleUser = userService.isGoogleUser(userEmail);
        int maxQuantity = isGoogleUser ? 10 : 2;
        String userType = isGoogleUser ? "Google" : "guest";

        if (order.getQuantity() > maxQuantity) {
            return "Limit Quantity to " + maxQuantity + " for " + userType + " users. If more boxes are required, please contact us at mangoes.bern@gmail.com.";
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        
        if ("guest@rama.local".equals(userEmail)) {
            return ResponseEntity.ok(List.of()); // Guests cannot view previous orders
        }

        Role role = userService.getRoleByEmail(userEmail);
        List<Order> orders = role == Role.ADMIN
                ? repository.findAll()
                : repository.findByUserEmail(userEmail);
        return ResponseEntity.ok(orders);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        Order order = repository.findById(id).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        Role role = userService.getRoleByEmail(userEmail);
        if (role == Role.ADMIN || order.getUserEmail().equals(userEmail)) {
            repository.delete(order);
            try {
                emailService.sendOrderDeletionNotification(order);
            } catch (Exception e) {
                System.err.println("Failed to send order deletion email: " + e.getMessage());
            }
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.status(403).build();
    }
}
