package com.rama.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository repository;
    private final UserService userService;

    public OrderController(OrderRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestBody Order order,
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        order.setUserEmail(userEmail);
        return ResponseEntity.ok(repository.save(order));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        Role role = userService.getRoleByEmail(userEmail);
        List<Order> orders = role == Role.ADMIN
                ? repository.findAll()
                : repository.findByUserEmail(userEmail);
        return ResponseEntity.ok(orders);
    }
}
