package com.acuver.kpl.orderservice.controller;

import com.acuver.kpl.orderservice.model.Order;
import com.acuver.kpl.orderservice.model.CreateOrderResponse;
import com.acuver.kpl.orderservice.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public Mono<CreateOrderResponse> createOrder(@RequestBody Order request) {
        log.info("inside api");
        return orderService.createOrder(request);
    }
}