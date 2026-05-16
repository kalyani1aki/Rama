package com.rama.backend;

import java.util.Map;

public class OrderStatsDTO {
    private long totalOrders;
    private long totalBoxes;
    private long confirmedOrders;
    private long confirmedBoxes;
    private long waitingOrders;
    private long waitingBoxes;
    private Map<String, Long> ordersPerLocation;
    private Map<String, Long> boxesPerLocation;
    private Map<String, Long> confirmedBoxesPerLocation;
    private Map<String, Long> waitingBoxesPerLocation;

    public OrderStatsDTO(long totalOrders, long totalBoxes, long confirmedOrders, long confirmedBoxes, long waitingOrders, long waitingBoxes, 
                        Map<String, Long> ordersPerLocation, Map<String, Long> boxesPerLocation,
                        Map<String, Long> confirmedBoxesPerLocation, Map<String, Long> waitingBoxesPerLocation) {
        this.totalOrders = totalOrders;
        this.totalBoxes = totalBoxes;
        this.confirmedOrders = confirmedOrders;
        this.confirmedBoxes = confirmedBoxes;
        this.waitingOrders = waitingOrders;
        this.waitingBoxes = waitingBoxes;
        this.ordersPerLocation = ordersPerLocation;
        this.boxesPerLocation = boxesPerLocation;
        this.confirmedBoxesPerLocation = confirmedBoxesPerLocation;
        this.waitingBoxesPerLocation = waitingBoxesPerLocation;
    }

    public long getTotalOrders() { return totalOrders; }
    public long getTotalBoxes() { return totalBoxes; }
    public long getConfirmedOrders() { return confirmedOrders; }
    public long getConfirmedBoxes() { return confirmedBoxes; }
    public long getWaitingOrders() { return waitingOrders; }
    public long getWaitingBoxes() { return waitingBoxes; }
    public Map<String, Long> getOrdersPerLocation() { return ordersPerLocation; }
    public Map<String, Long> getBoxesPerLocation() { return boxesPerLocation; }
    public Map<String, Long> getConfirmedBoxesPerLocation() { return confirmedBoxesPerLocation; }
    public Map<String, Long> getWaitingBoxesPerLocation() { return waitingBoxesPerLocation; }
}
