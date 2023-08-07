package com.acuver.kpl.orderservice.model;

import lombok.Data;

@Data
public class CreateOrderRequest {

    private String productId;
    private Integer quantity;
}
