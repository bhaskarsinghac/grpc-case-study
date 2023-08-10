package com.acuver.kpl.inventoryservice.service;

import com.acuver.kpl.inventory_components.InventoryServiceGrpc;
import com.acuver.kpl.inventory_components.ReserveInventoryRequest;
import com.acuver.kpl.inventory_components.ReserveInventoryResponse;
import com.acuver.kpl.inventoryservice.repository.InventoryRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Arrays;

@GrpcService
@Slf4j
public class InventoryService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void reserveInventory(ReserveInventoryRequest request, StreamObserver<ReserveInventoryResponse> responseObserver) {

        log.info("Inside reserveInventory");
        ReserveInventoryResponse response;

        try {
            response = ReserveInventoryResponse.newBuilder()
                    .setProductId(request.getProductId())
                    .setSuccess(true)
                    .build();
        } catch (Exception e) {
            responseObserver.onError(Status.UNKNOWN
                    .withDescription(Arrays.toString(e.getStackTrace()))
                    .asRuntimeException());
            return;
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}