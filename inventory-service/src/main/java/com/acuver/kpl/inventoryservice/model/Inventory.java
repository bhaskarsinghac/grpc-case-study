package com.acuver.kpl.inventoryservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("inventory")
@Data
public class Inventory {
    @Id
    private String productId;
    private String productName;
    private Integer availableQuantity;
    private Integer reservedQuantities;
}