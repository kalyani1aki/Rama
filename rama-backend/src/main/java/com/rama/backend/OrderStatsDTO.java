package com.rama.backend;

import java.util.Map;

public class OrderStatsDTO {
    private long totalOrders;
    private long totalBoxes;
    private Map<String, Long> ordersPerLocation;
    private Map<String, Long> boxesPerLocation;

    public OrderStatsDTO(long totalOrders, long totalBoxes, Map<String, Long> ordersPerLocation, Map<String, Long> boxesPerLocation) {
        this.totalOrders = totalOrders;
        this.totalBoxes = totalBoxes;
        this.ordersPerLocation = ordersPerLocation;
        this.boxesPerLocation = boxesPerLocation;
    }

    public long getTotalOrders() { return totalOrders; }
    public long getTotalBoxes() { return totalBoxes; }
    public Map<String, Long> getOrdersPerLocation() { return ordersPerLocation; }
    public Map<String, Long> getBoxesPerLocation() { return boxesPerLocation; }
}
