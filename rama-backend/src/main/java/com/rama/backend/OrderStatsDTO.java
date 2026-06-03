package com.rama.backend;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class OrderStatsDTO {
    @JsonProperty("totalOrders")
    private long totalOrders;
    
    @JsonProperty("totalBoxes")
    private long totalBoxes;
    
    @JsonProperty("confirmedOrders")
    private long confirmedOrders;
    
    @JsonProperty("confirmedBoxes")
    private long confirmedBoxes;
    
    @JsonProperty("waitingOrders")
    private long waitingOrders;
    
    @JsonProperty("waitingBoxes")
    private long waitingBoxes;
    
    @JsonProperty("paidBoxes")
    private long paidBoxes;
    
    @JsonProperty("pickedUpBoxes")
    private long pickedUpBoxes;
    
    @JsonProperty("totalRevenue")
    private double totalRevenue;
    
    @JsonProperty("paidRevenue")
    private double paidRevenue;

    @JsonProperty("ordersPerLocation")
    private Map<String, Long> ordersPerLocation;
    
    @JsonProperty("boxesPerLocation")
    private Map<String, Long> boxesPerLocation;
    
    @JsonProperty("confirmedBoxesPerLocation")
    private Map<String, Long> confirmedBoxesPerLocation;
    
    @JsonProperty("waitingBoxesPerLocation")
    private Map<String, Long> waitingBoxesPerLocation;
    
    @JsonProperty("paidBoxesPerLocation")
    private Map<String, Long> paidBoxesPerLocation;
    
    @JsonProperty("pickedUpBoxesPerLocation")
    private Map<String, Long> pickedUpBoxesPerLocation;

    public OrderStatsDTO(long totalOrders, long totalBoxes, long confirmedOrders, long confirmedBoxes, long waitingOrders, long waitingBoxes, 
                        long paidBoxes, long pickedUpBoxes, double totalRevenue, double paidRevenue,
                        Map<String, Long> ordersPerLocation, Map<String, Long> boxesPerLocation,
                        Map<String, Long> confirmedBoxesPerLocation, Map<String, Long> waitingBoxesPerLocation,
                        Map<String, Long> paidBoxesPerLocation, Map<String, Long> pickedUpBoxesPerLocation) {
        this.totalOrders = totalOrders;
        this.totalBoxes = totalBoxes;
        this.confirmedOrders = confirmedOrders;
        this.confirmedBoxes = confirmedBoxes;
        this.waitingOrders = waitingOrders;
        this.waitingBoxes = waitingBoxes;
        this.paidBoxes = paidBoxes;
        this.pickedUpBoxes = pickedUpBoxes;
        this.totalRevenue = totalRevenue;
        this.paidRevenue = paidRevenue;
        this.ordersPerLocation = ordersPerLocation;
        this.boxesPerLocation = boxesPerLocation;
        this.confirmedBoxesPerLocation = confirmedBoxesPerLocation;
        this.waitingBoxesPerLocation = waitingBoxesPerLocation;
        this.paidBoxesPerLocation = paidBoxesPerLocation;
        this.pickedUpBoxesPerLocation = pickedUpBoxesPerLocation;
    }

    public long getTotalOrders() { return totalOrders; }
    public long getTotalBoxes() { return totalBoxes; }
    public long getConfirmedOrders() { return confirmedOrders; }
    public long getConfirmedBoxes() { return confirmedBoxes; }
    public long getWaitingOrders() { return waitingOrders; }
    public long getWaitingBoxes() { return waitingBoxes; }
    public long getPaidBoxes() { return paidBoxes; }
    public long getPickedUpBoxes() { return pickedUpBoxes; }
    public double getTotalRevenue() { return totalRevenue; }
    public double getPaidRevenue() { return paidRevenue; }
    public Map<String, Long> getOrdersPerLocation() { return ordersPerLocation; }
    public Map<String, Long> getBoxesPerLocation() { return boxesPerLocation; }
    public Map<String, Long> getConfirmedBoxesPerLocation() { return confirmedBoxesPerLocation; }
    public Map<String, Long> getWaitingBoxesPerLocation() { return waitingBoxesPerLocation; }
    public Map<String, Long> getPaidBoxesPerLocation() { return paidBoxesPerLocation; }
    public Map<String, Long> getPickedUpBoxesPerLocation() { return pickedUpBoxesPerLocation; }
}
