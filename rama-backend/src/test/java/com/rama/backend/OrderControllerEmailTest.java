package com.rama.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderControllerEmailTest {

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
    void createOrder_shouldSendEmail() {
        Order order = new Order("Test User", "Address", "123456", 1, "Location");
        order.setUserEmail("test@example.com");
        
        when(orderRepository.existsByUserEmail(any())).thenReturn(false);
        when(orderRepository.save(any())).thenReturn(order);
        when(userService.isGoogleUser(any())).thenReturn(true);

        orderController.createOrder(order, "test@example.com");

        verify(emailService, times(1)).sendOrderConfirmation(any());
    }

    @Test
    void updateOrder_shouldSendEmail() {
        Order order = new Order("Test User", "Address", "123456", 1, "Location");
        order.setUserEmail("test@example.com");
        
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(userService.getRoleByEmail(any())).thenReturn(Role.USER);
        when(userService.isGoogleUser(any())).thenReturn(true);

        orderController.updateOrder("1", order, "test@example.com");

        verify(emailService, times(1)).sendOrderUpdateNotification(any());
    }

    @Test
    void deleteOrder_shouldSendEmail() {
        Order order = new Order("Test User", "Address", "123456", 1, "Location");
        order.setUserEmail("test@example.com");
        
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(userService.getRoleByEmail(any())).thenReturn(Role.USER);

        orderController.deleteOrder("1", "test@example.com");

        verify(emailService, times(1)).sendOrderDeletionNotification(any());
    }

    @Test
    void createOrder_withHighQuantity_shouldSucceed() {
        Order order = new Order("Admin User", "Address", "123456", 999, "Location");
        order.setUserEmail("admin@example.com");
        
        when(orderRepository.existsByUserEmail(any())).thenReturn(false);
        when(orderRepository.save(any())).thenReturn(order);

        ResponseEntity<?> response = orderController.createOrder(order, "admin@example.com");

        assertEquals(200, response.getStatusCode().value());
        verify(emailService, times(1)).sendOrderConfirmation(any());
    }

    @Test
    void createOrder_whenSoldOut_shouldSendWaitingListEmail() {
        Order order = new Order("Test User", "Address", "123456", 1, "Location");
        order.setUserEmail("test@example.com");
        
        AppConfig soldOutConfig = new AppConfig("SOLD_OUT", "true");
        when(configRepository.findById("SOLD_OUT")).thenReturn(Optional.of(soldOutConfig));
        when(orderRepository.existsByUserEmail(any())).thenReturn(false);
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userService.getRoleByEmail(any())).thenReturn(Role.USER);

        ResponseEntity<?> response = orderController.createOrder(order, "test@example.com");

        assertEquals(200, response.getStatusCode().value());
        Order savedOrder = (Order) response.getBody();
        assertEquals(OrderStatus.WAITING, savedOrder.getStatus());
        verify(emailService, times(1)).sendWaitingListConfirmation(any());
        verify(emailService, never()).sendOrderConfirmation(any());
    }
}
