package com.acuver.kpl.orderservice.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateOrderResponse {

    private String productId;
    private Boolean status;
}
