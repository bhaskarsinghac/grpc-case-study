package com.acuver.kpl.orderservice.service;

import com.acuver.kpl.inventory_components.*;
import com.acuver.kpl.orderservice.model.CreateOrderResponse;
import com.acuver.kpl.orderservice.model.Order;
import com.acuver.kpl.orderservice.repository.OrderRepository;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class OrderService {

    @GrpcClient("order-service")
    InventoryServiceGrpc.InventoryServiceFutureStub futureStub;

    @Autowired
    OrderRepository orderRepository;
    final ExecutorService executor = Executors.newCachedThreadPool();

    public Mono<CreateOrderResponse> createOrder(Order createOrderReq) {
        orderRepository.save(createOrderReq).subscribe();

        var invReserveReqBuilder = ReserveInventoryRequest.newBuilder();
        for (Order.OrderLine orderLine : createOrderReq.getOrderLineEntries()) {
            var orderLineObj = ReserveInventory.newBuilder()
                    .setProductId(orderLine.getSku())
                    .setOrderId(orderLine.getSellerOrderId())
                    .setQuantity(orderLine.getQuantity())
                    .build();
            invReserveReqBuilder.addInventory(orderLineObj);
        }
        var invReserveReq = invReserveReqBuilder.build();

        var invReserveResFuture = futureStub
                .withDeadlineAfter(25, TimeUnit.SECONDS).reserveInventory(invReserveReq);

        return Mono.create(monoSink -> Futures.addCallback(invReserveResFuture, new FutureCallback<>() {

            @Override
            public void onSuccess(@NullableDecl ReserveInventoryListResponse invReserveRes) {
                log.warn("Inside Success");
                AtomicInteger reservedCounter = new AtomicInteger();
                try {
                    Mono.just(createOrderReq).doOnSuccess(order -> {
                                for (Order.OrderLine orderLine : order.getOrderLineEntries()) {
                                    for (ReserveInventoryResponse response : invReserveRes.getInvResponseList()) {
                                        if (orderLine.getSku().equals(response.getProductId())) {
                                            if (response.getSuccess()) {
                                                orderLine.setStatus("RESERVED");
                                                reservedCounter.addAndGet(1);
                                            } else {
                                                orderLine.setStatus("CANCELLED");
                                                orderLine.setCancellationReason("Insufficient Inventory");
                                            }
                                            break;
                                        }
                                    }
                                }
//                                orderRepository.save(order).subscribe();
                            }).doOnError(throwable -> log.error(throwable.getMessage()))
                            .subscribe();

                    CreateOrderResponse res = new CreateOrderResponse();
                    res.setSellerOrderId(createOrderReq.getSellerOrderId());
                    if (reservedCounter.get() == createOrderReq.getOrderLineEntries().size()) {
                        res.setOrderStatus("CONFIRMED");
                        res.setCancellationReason(null);
                        monoSink.success(res);
                        createOrderReq.setStatus("CONFIRMED");
                        orderRepository.save(createOrderReq).subscribe();
                    } else {
                        res.setOrderStatus("CANCELLED");
                        res.setCancellationReason("Unable to reserve inventory");
                        monoSink.success(res);
                        createOrderReq.setStatus("CANCELLED");
                        orderRepository.save(createOrderReq).subscribe();
                    }
                } catch (Exception e) {
                    monoSink.error(e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Inside Failure");
                t.printStackTrace();
                monoSink.error(new Exception(t.getMessage()));
            }
        }, executor));
    }
}