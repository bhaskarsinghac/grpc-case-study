package com.acuver.kpl.inventoryservice.service;

import com.acuver.kpl.inventory_components.InventoryServiceGrpc;
import com.acuver.kpl.inventory_components.ReserveInventoryRequest;
import com.acuver.kpl.inventory_components.ReserveInventoryResponse;
import com.acuver.kpl.inventoryservice.repository.InventoryRepository;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@Slf4j
public class InventoryService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void reserveInventory(ReserveInventoryRequest request, StreamObserver<ReserveInventoryResponse> responseObserver) {

        var productId = request.getProductId();

        log.warn("IN reserveInventory");
//        log.warn(String.valueOf(request));
        ReserveInventoryResponse response = ReserveInventoryResponse.newBuilder()
                .setSuccess(true)
                .build();
//        log.warn(String.valueOf(response));
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}