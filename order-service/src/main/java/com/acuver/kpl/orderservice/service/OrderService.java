package com.acuver.kpl.orderservice.service;

import com.acuver.kpl.inventory_components.InventoryServiceGrpc;
import com.acuver.kpl.inventory_components.ReserveInventory;
import com.acuver.kpl.inventory_components.ReserveInventoryListResponse;
import com.acuver.kpl.inventory_components.ReserveInventoryRequest;
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
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Objects;
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

    public Mono<Object> createOrder(Order createOrderReq) {
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


        return Mono.create(monoSink -> {

                    ListenableFuture<ReserveInventoryListResponse> invReserveResFuture = futureStub
                            .withDeadlineAfter(5, TimeUnit.SECONDS).reserveInventory(invReserveReq);
                    Futures.addCallback(invReserveResFuture, new FutureCallback<>() {

                        @Override
                        public void onSuccess(@NullableDecl ReserveInventoryListResponse invReserveRes) {
                            log.info("Inside onSuccess");

                            AtomicInteger reservedCounter = new AtomicInteger();
                            Mono.just(createOrderReq)
                                    .flatMap(order -> updaterOrderLineEntryStatus(Objects.requireNonNull(invReserveRes), reservedCounter, order))
                                    .doOnSuccess(order -> updateOuterOrderStatus(reservedCounter, createOrderReq, monoSink))
                                    .doOnError(t -> handleError(monoSink, new Exception("Error in OrderService", t)))
                                    .doFinally(signalType -> {
                                        if (signalType == SignalType.ON_COMPLETE) {  // Check if it's successful
                                            orderRepository.save(createOrderReq).subscribe(); // Save only on success
                                        }
                                    })
                                    .subscribe();
                        }

                        @Override
                        public void onFailure(@NullableDecl Throwable t) {
                            log.error("Inside onFailure");
                            handleError(monoSink, new Exception("Error while communicating with InventoryService", t));
                        }

                    }, executor);
                }).retryWhen(Retry.backoff(3, Duration.ofSeconds(3))
                        .doAfterRetry(signal -> log.info("Retry Attempt {}. Status: {}",
                                signal.totalRetries() + 1, signal.failure().getMessage()))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure()))
                .log();
    }

    private Mono<Order> updaterOrderLineEntryStatus(ReserveInventoryListResponse invReserveRes, AtomicInteger reservedCounter, Order order) {
        return Flux.zip(Flux.fromIterable(order.getOrderLineEntries()),
                        Flux.fromIterable(invReserveRes.getInvResponseList()),
                        (orderLine, reserveResponse) -> {
                            if (reserveResponse.getProductId().equals(orderLine.getSku())) {
                                if (reserveResponse.getSuccess()) {
                                    orderLine.setStatus("RESERVED");
                                    reservedCounter.incrementAndGet();
                                } else {
                                    orderLine.setStatus("CANCELLED");
                                    orderLine.setCancellationReason("Insufficient Inventory");
                                }
                            }
                            return orderLine;
                        })
                .collectList()
                .flatMap(orderLines -> {
                    order.setOrderLineEntries(orderLines);
                    return Mono.just(order);
                });
    }

    private void updateOuterOrderStatus(AtomicInteger reservedCounter, Order createOrderReq, MonoSink<Object> monoSink) {
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

    private void handleError(MonoSink<Object> monoSink, Exception e) {
        monoSink.error(e);
        e.printStackTrace();
    }
}