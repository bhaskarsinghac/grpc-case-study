package com.acuver.kpl.orderservice.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
//@Builder
public class CreateOrderResponse {

    private List<OrderLineRes> orderLineResList;


    @Getter
    @Setter
    public static class OrderLineRes {
        private String productId;
        private String OrderId;
        private Boolean status;
    }
}
