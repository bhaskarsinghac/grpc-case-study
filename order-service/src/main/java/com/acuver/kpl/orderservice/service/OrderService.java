package com.acuver.kpl.orderservice.service;

import com.acuver.kpl.inventory_components.InventoryServiceGrpc;
import com.acuver.kpl.inventory_components.ReserveInventoryRequest;
import com.acuver.kpl.inventory_components.ReserveInventoryResponse;
import com.acuver.kpl.order_components.CreateOrderReq;
import com.acuver.kpl.order_components.CreateOrderRes;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    @GrpcClient("order-service")
    InventoryServiceGrpc.InventoryServiceBlockingStub blockingStub;

    public CreateOrderRes createOrder(CreateOrderReq req) {

        var invReserveReq = ReserveInventoryRequest.newBuilder()
                .setProductId(req.getProductId())
                .setQuantity(req.getQuantity())
                .build();
        ReserveInventoryResponse invReserveRes = blockingStub.reserveInventory(invReserveReq);

        return CreateOrderRes.newBuilder()
                .setProductId(invReserveRes.getProductId())
                .setSuccess(invReserveRes.getSuccess())
                .build();
    }
}