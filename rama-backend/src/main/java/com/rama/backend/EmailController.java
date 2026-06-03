package com.rama.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/emails")
public class EmailController {

    private final EmailService emailService;
    private final UserService userService;
    private final OrderRepository orderRepository;

    public EmailController(EmailService emailService, UserService userService, OrderRepository orderRepository) {
        this.emailService = emailService;
        this.userService = userService;
        this.orderRepository = orderRepository;
    }
/*
    @PostMapping("/generic")
    public ResponseEntity<?> sendGenericEmail(
            @RequestBody GenericEmailRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        
        if (userService.getRoleByEmail(userEmail) != Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        List<String> emails = orderRepository.findAllUniqueEmails();
        emailService.sendGenericEmail(emails, request.subject(), request.body());
        return ResponseEntity.ok().build();
    }*/

    @PostMapping("/pickup")
    public ResponseEntity<?> sendPickupEmails(
            @RequestBody List<PickupInfoRequest> requests,
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        
        if (userService.getRoleByEmail(userEmail) != Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        List<Order> confirmedOrders = orderRepository.findConfirmedOrders();

        for (PickupInfoRequest request : requests) {
            List<String> emails = confirmedOrders.stream()
                    .filter(o -> o.getPickupLocation().equals(request.location()))
                    .map(Order::getUserEmail)
                    .distinct()
                    .collect(Collectors.toList());

            if (!emails.isEmpty()) {
                emailService.sendPickupConfirmationEmail(emails, request.location(), request.time(), request.contactPerson(), request.contactPhone());
            }
        }

        return ResponseEntity.ok().build();
        }

        record GenericEmailRequest(String subject, String body) {}
        record PickupInfoRequest(String location, String time, String contactPerson, String contactPhone) {}
        }
