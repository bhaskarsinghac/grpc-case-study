package com.acuver.kpl.orderservice.service;

import com.acuver.kpl.inventory_components.*;
import com.acuver.kpl.orderservice.model.CreateOrderResponse;
import com.acuver.kpl.orderservice.model.Order;
import com.acuver.kpl.orderservice.repository.OrderRepository;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.SignalType;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

        ListenableFuture<ReserveInventoryListResponse> invReserveResFuture = futureStub
                .withDeadlineAfter(25, TimeUnit.SECONDS).reserveInventory(invReserveReq);

        return Mono.create(monoSink -> Futures.addCallback(invReserveResFuture, new FutureCallback<>() {

            @Override
            public void onSuccess(@NullableDecl ReserveInventoryListResponse invReserveRes) {
                log.warn("Inside Success");

                AtomicInteger reservedCounter = new AtomicInteger();
                Mono.just(createOrderReq)
                        .flatMap(order -> updaterOrderLineEntryStatus(Objects.requireNonNull(invReserveRes), reservedCounter, order))
                        .doOnSuccess(order -> updateOuterOrderStatus(reservedCounter, createOrderReq, monoSink))
                        .doOnError(t -> handleError(t, monoSink))
                        .doFinally(signalType -> {
                            if (signalType == SignalType.ON_COMPLETE) {  // Check if it's successful
                                orderRepository.save(createOrderReq).subscribe(); // Save only on success
                            }
                        })
                        .subscribe();
            }

            @Override
            public void onFailure(@NullableDecl Throwable t) {
                handleError(t, monoSink);
            }
        }, executor));
    }

    private void handleError(Throwable t, MonoSink<CreateOrderResponse> monoSink) {
        log.error("Inside doOnError");
        monoSink.error(t);
        t.printStackTrace();
    }

    private Mono<Order> updaterOrderLineEntryStatus(ReserveInventoryListResponse invReserveRes, AtomicInteger reservedCounter, Order order) {

        return Flux.fromIterable(order.getOrderLineEntries())
                .flatMap(orderLine -> Mono.justOrEmpty(invReserveRes.getInvResponseList().stream()
                                .filter(reserveResponse -> reserveResponse.getProductId().equals(orderLine.getSku()))
                                .findFirst())
                        .doOnNext(reserveResponse -> {
                            if (reserveResponse.getSuccess()) {
                                orderLine.setStatus("RESERVED");
                                reservedCounter.incrementAndGet();
                            } else {
                                orderLine.setStatus("CANCELLED");
                                orderLine.setCancellationReason("Insufficient Inventory");
                            }
                        })).then(Mono.just(order));
    }

    private void updateOuterOrderStatus(AtomicInteger reservedCounter, Order createOrderReq, MonoSink<CreateOrderResponse> monoSink) {
        CreateOrderResponse res = new CreateOrderResponse();
        res.setSellerOrderId(createOrderReq.getSellerOrderId());
        if (reservedCounter.get() == createOrderReq.getOrderLineEntries().size()) {
            res.setOrderStatus("CONFIRMED");
            res.setCancellationReason(null);
            createOrderReq.setStatus("CONFIRMED");
        } else {
            res.setOrderStatus("CANCELLED");
            res.setCancellationReason("Unable to reserve inventory");
            createOrderReq.setStatus("CANCELLED");
        }
        monoSink.success(res);
    }
}