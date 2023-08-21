package com.acuver.kpl.orderservice.repository;

import com.acuver.kpl.orderservice.model.Order;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface OrderRepository extends ReactiveMongoRepository<Order, String> {

    Mono<Order> findBySellerOrderId(String sellerOrderId);
}
