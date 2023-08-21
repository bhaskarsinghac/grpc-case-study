package com.acuver.kpl.orderservice.service;

import com.acuver.kpl.inventory_components.*;
import com.acuver.kpl.orderservice.model.Order;
import com.acuver.kpl.orderservice.model.CreateOrderResponse;
import com.acuver.kpl.orderservice.repository.OrderRepository;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OrderService {

    @GrpcClient("order-service")
    InventoryServiceGrpc.InventoryServiceFutureStub futureStub;

    @Autowired
    OrderRepository orderRepository;
    final ExecutorService executor = Executors.newCachedThreadPool();

    public Mono<CreateOrderResponse> createOrder(Order req) {
        orderRepository.save(req).subscribe();

        var invReserveReqBuilder = ReserveInventoryRequest.newBuilder();
        for(Order.OrderLine orderLine: req.getOrderLineEntries() ){
            var orderLineObj = ReserveInventory.newBuilder()
                    .setProductId(orderLine.getSku())
                    .setOrderId(orderLine.getSellerOrderId())
                    .setQuantity(orderLine.getQuantity())
                    .build();
            invReserveReqBuilder.addInventory(orderLineObj);
        }
        var invReserveReq = invReserveReqBuilder.build();

        var invReserveResFuture = futureStub
                .withDeadlineAfter(10, TimeUnit.SECONDS).reserveInventory(invReserveReq);

        return Mono.create(monoSink -> Futures.addCallback(invReserveResFuture, new FutureCallback<>() {

            @Override
            public void onSuccess(@NullableDecl ReserveInventoryListResponse invReserveRes) {
                log.warn("Inside Success");
                try {
                    orderRepository.findBySellerOrderId(invReserveRes.getInvResponse(0).getOrderId()).doOnSuccess(order ->{
                        for(Order.OrderLine orderLine: order.getOrderLineEntries() ){
                            for(ReserveInventoryResponse response: invReserveRes.getInvResponseList()){
                                if(orderLine.getSku().equals(response.getProductId())){
                                    if(response.getSuccess())
                                        orderLine.setStatus("CONFIRMED");
                                    else {
                                        orderLine.setStatus("CANCELLED");
                                        orderLine.setCancellationReason("Insufficient Inventory");
                                    }
                                    break;
                                }
                            }
                        }
                        orderRepository.save(order).subscribe();
                    }).doOnError(throwable -> log.error(throwable.getMessage()))
                            .subscribe();

                    List<CreateOrderResponse.OrderLineRes> orderLineResList  = new ArrayList<>();
                    for(ReserveInventoryResponse response: invReserveRes.getInvResponseList()){
                        CreateOrderResponse.OrderLineRes orderLineRes = new CreateOrderResponse.OrderLineRes();
                        orderLineRes.setProductId(response.getProductId());
                        orderLineRes.setOrderId(response.getOrderId());
                        orderLineRes.setStatus(response.getSuccess());
                        orderLineResList.add(orderLineRes);

                    }
                    CreateOrderResponse createOrderResponse = new CreateOrderResponse();
                    createOrderResponse.setOrderLineResList(orderLineResList);
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