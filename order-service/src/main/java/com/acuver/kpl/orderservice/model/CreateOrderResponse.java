package com.acuver.kpl.orderservice.model;


import lombok.Data;

@Data
public class CreateOrderResponse {

    private String sellerOrderId;
    private String orderStatus;
    private String cancellationReason;

}
