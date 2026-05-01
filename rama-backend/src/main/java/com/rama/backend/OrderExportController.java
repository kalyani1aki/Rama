package com.rama.backend;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderExportController {

    private final OrderRepository orderRepository;
    private final UserService userService;

    public OrderExportController(OrderRepository orderRepository, UserService userService) {
        this.orderRepository = orderRepository;
        this.userService = userService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrders(
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {

        Role role = userService.getRoleByEmail(userEmail);
        List<Order> orders = role == Role.ADMIN
                ? orderRepository.findAll()
                : orderRepository.findByUserEmail(userEmail);

        try (SXSSFWorkbook wb = new SXSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Orders");

            Row header = sheet.createRow(0);
            String[] columns = {"ID", "User Email", "Name", "Address", "Phone", "Quantity", "Pickup Location", "Created At"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int rowNum = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(order.getId() != null ? order.getId() : "");
                row.createCell(1).setCellValue(order.getUserEmail() != null ? order.getUserEmail() : "");
                row.createCell(2).setCellValue(order.getName());
                row.createCell(3).setCellValue(order.getAddress());
                row.createCell(4).setCellValue(order.getPhone());
                row.createCell(5).setCellValue(order.getQuantity());
                row.createCell(6).setCellValue(order.getPickupLocation() != null ? order.getPickupLocation() : "");
                row.createCell(7).setCellValue(order.getCreatedAt() != null ? order.getCreatedAt().toString() : "");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
