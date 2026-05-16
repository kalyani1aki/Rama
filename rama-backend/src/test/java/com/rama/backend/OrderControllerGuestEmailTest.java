package com.rama.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderControllerGuestEmailTest {

    private OrderController orderController;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @Mock
    private AppConfigRepository configRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderController = new OrderController(orderRepository, userService, emailService, configRepository);
    }

    @Test
    void createGuestOrder_shouldSendEmailToGuest() {
        // Simulating the order object sent by frontend for a guest
        Order order = new Order("Guest User", "Address", "1234567890", 1, "Location");
        order.setUserEmail("guest@example.com"); // Email provided in the form field

        when(orderRepository.existsByUserEmail(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userService.isGoogleUser(anyString())).thenReturn(false);
        when(userService.getRoleByEmail(anyString())).thenReturn(Role.USER);

        // Header passed by frontend for guest is typically "guest@rama.local"
        ResponseEntity<?> response = orderController.createOrder(order, "guest@rama.local");

        assertEquals(200, response.getStatusCode().value());
        
        // Capture the order passed to emailService
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(emailService, times(1)).sendOrderConfirmation(orderCaptor.capture());
        
        assertEquals("guest@example.com", orderCaptor.getValue().getUserEmail());
    }
}
