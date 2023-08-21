package com.acuver.kpl.orderservice.service;

import com.acuver.kpl.inventory_components.InventoryServiceGrpc;
import com.acuver.kpl.inventory_components.ReserveInventoryRequest;
import com.acuver.kpl.inventory_components.ReserveInventoryResponse;
import com.acuver.kpl.orderservice.model.CreateOrderRequest;
import com.acuver.kpl.orderservice.model.CreateOrderResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OrderService {

    @GrpcClient("order-service")
    InventoryServiceGrpc.InventoryServiceFutureStub futureStub;
    final ExecutorService executor = Executors.newCachedThreadPool();

    public Mono<CreateOrderResponse> createOrder(CreateOrderRequest req) {

        var invReserveReq = ReserveInventoryRequest.newBuilder()
                .setProductId(req.getProductId())
                .setQuantity(req.getQuantity())
                .build();

        var invReserveResFuture = futureStub
                .withDeadlineAfter(10, TimeUnit.SECONDS).reserveInventory(invReserveReq);

        return Mono.create(monoSink -> Futures.addCallback(invReserveResFuture, new FutureCallback<>() {

            @Override
            public void onSuccess(@NullableDecl ReserveInventoryResponse invReserveRes) {
                log.warn("Inside Success");
                try {
                    var createOrderResponse = CreateOrderResponse.builder()
                            .productId(invReserveRes.getProductId())
                            .status(invReserveRes.getSuccess())
                            .build();
                    monoSink.success(createOrderResponse);
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