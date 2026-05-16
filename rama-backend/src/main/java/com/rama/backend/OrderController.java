package com.rama.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository repository;
    private final UserService userService;
    private final EmailService emailService;
    private final AppConfigRepository configRepository;

    public OrderController(OrderRepository repository, UserService userService, EmailService emailService, AppConfigRepository configRepository) {
        this.repository = repository;
        this.userService = userService;
        this.emailService = emailService;
        this.configRepository = configRepository;
    }

    private boolean isSoldOut() {
        return configRepository.findById("SOLD_OUT")
                .map(config -> Boolean.parseBoolean(config.getConfigValue()))
                .orElse(false);
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody Order order,
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        
        String effectiveEmail = "guest@rama.local".equals(userEmail) || "guest".equals(userEmail)
                ? order.getUserEmail() 
                : userEmail;

        Role role = userService.getRoleByEmail(effectiveEmail);
        boolean soldOut = isSoldOut();
        
        // ADMINs are never restricted by sold out
        if (role != Role.ADMIN && soldOut) {
            order.setStatus(OrderStatus.WAITING);
        } else {
            order.setStatus(OrderStatus.CONFIRMED);
        }

        System.out.println("DEBUG: Creating order. Header email: " + userEmail + ", Body email: " + order.getUserEmail() + ", Effective email: " + effectiveEmail);

        if (effectiveEmail == null || effectiveEmail.trim().isEmpty() || "guest".equals(effectiveEmail) || "guest@rama.local".equals(effectiveEmail)) {
            System.out.println("DEBUG: Validation failed - Email is mandatory");
            return ResponseEntity.badRequest().body("Email is mandatory for all users.");
        }

        if (repository.existsByUserEmail(effectiveEmail)) {
            return ResponseEntity.badRequest().body("Order NOT placed! An order is already recorded for " + effectiveEmail + ". For changes or updates, please contact us at mangoes.bern@gmail.com.");
        }

        order.setUserEmail(effectiveEmail);
        Order savedOrder = repository.save(order);
        try {
            if (savedOrder.getStatus() == OrderStatus.WAITING) {
                emailService.sendWaitingListConfirmation(savedOrder);
            } else {
                emailService.sendOrderConfirmation(savedOrder);
            }
        } catch (Exception e) {
            System.err.println("Failed to send order confirmation email: " + e.getMessage());
        }
        return ResponseEntity.ok(savedOrder);
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmOrder(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        
        Role role = userService.getRoleByEmail(userEmail);
        if (role != Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        Order order = repository.findById(id).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (order.getStatus() != OrderStatus.WAITING) {
            return ResponseEntity.badRequest().body("Only waiting orders can be confirmed.");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = repository.save(order);
        try {
            emailService.sendOrderConfirmedFromWaitingList(savedOrder);
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
        if (role != Role.ADMIN && (isSoldOut() || !order.getUserEmail().equals(userEmail))) {
            return ResponseEntity.status(403).build();
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

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        
        Role role = userService.getRoleByEmail(userEmail);
        if (role != Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        List<Order> allOrders = repository.findAll();
        
        long totalOrders = allOrders.size();
        long totalBoxes = allOrders.stream().mapToLong(Order::getQuantity).sum();

        long confirmedOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED || o.getStatus() == null)
                .count();
        long confirmedBoxes = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED || o.getStatus() == null)
                .mapToLong(Order::getQuantity).sum();

        long waitingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.WAITING)
                .count();
        long waitingBoxes = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.WAITING)
                .mapToLong(Order::getQuantity).sum();
        
        Map<String, Long> ordersPerLocation = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getPickupLocation, Collectors.counting()));
        
        Map<String, Long> boxesPerLocation = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getPickupLocation, Collectors.summingLong(Order::getQuantity)));

        Map<String, Long> confirmedBoxesPerLocation = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED || o.getStatus() == null)
                .collect(Collectors.groupingBy(Order::getPickupLocation, Collectors.summingLong(Order::getQuantity)));

        Map<String, Long> waitingBoxesPerLocation = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.WAITING)
                .collect(Collectors.groupingBy(Order::getPickupLocation, Collectors.summingLong(Order::getQuantity)));

        return ResponseEntity.ok(new OrderStatsDTO(totalOrders, totalBoxes, confirmedOrders, confirmedBoxes, waitingOrders, waitingBoxes, 
                ordersPerLocation, boxesPerLocation, confirmedBoxesPerLocation, waitingBoxesPerLocation));
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
        if (role == Role.ADMIN || (order.getUserEmail().equals(userEmail) && !isSoldOut())) {
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
