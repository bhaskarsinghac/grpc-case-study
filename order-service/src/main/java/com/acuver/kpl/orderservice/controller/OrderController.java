package com.acuver.kpl.orderservice.controller;

import com.acuver.kpl.order_components.CreateOrderReq;
import com.acuver.kpl.order_components.CreateOrderRes;
import com.acuver.kpl.orderservice.model.CreateOrderRequest;
import com.acuver.kpl.orderservice.model.CreateOrderResponse;
import com.acuver.kpl.orderservice.service.OrderService;
import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<CreateOrderResponse>> createOrder(@RequestBody CreateOrderRequest request) throws InvalidProtocolBufferException {
        // Call the OrderService to create the order
        var createOrderReq = CreateOrderReq.newBuilder()
                .setProductId(request.getProductId())
                .setQuantity(request.getQuantity())
                .build();
        var res = orderService.createOrder(createOrderReq);
        var createOrderResponse = CreateOrderResponse.builder()
                .productId(request.getProductId())
                .status(res.getSuccess())
                .build();
        return Mono.just(ResponseEntity.ok(createOrderResponse));
    }
}