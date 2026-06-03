package com.rama.backend;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String userEmail;
    private String name;
    private String address;
    private String phone;
    private int quantity;
    private String pickupLocation;
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.CONFIRMED;

    @JsonProperty("isPaid")
    private Boolean isPaid = false;

    @JsonProperty("isPickedUp")
    private Boolean isPickedUp = false;

    public Order() {}

    public Order(String name, String address, String phone, int quantity, String pickupLocation) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.quantity = quantity;
        this.pickupLocation = pickupLocation;
    }

    public String getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public boolean isPaid() { return isPaid != null && isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }

    public boolean isPickedUp() { return isPickedUp != null && isPickedUp; }
    public void setPickedUp(boolean pickedUp) { isPickedUp = pickedUp; }
}
