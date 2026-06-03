package com.rama.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserEmail(String userEmail);
    boolean existsByUserEmail(String userEmail);
    List<Order> findByStatus(OrderStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE o.status IS NULL OR o.status = 'CONFIRMED'")
    List<Order> findConfirmedOrders();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT o.userEmail FROM Order o")
    List<String> findAllUniqueEmails();
}
