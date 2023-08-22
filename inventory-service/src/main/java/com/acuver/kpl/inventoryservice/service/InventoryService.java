package com.acuver.kpl.inventoryservice.service;

import com.acuver.kpl.inventory_components.*;
import com.acuver.kpl.inventoryservice.model.Inventory;
import com.acuver.kpl.inventoryservice.repository.InventoryRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Arrays;
import java.util.Optional;

@GrpcService
@Slf4j
public class InventoryService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void reserveInventory(ReserveInventoryRequest request, StreamObserver<ReserveInventoryListResponse> responseObserver) {
        log.info("Inside reserveInventory");

        var response = ReserveInventoryListResponse.newBuilder();
        try {
            for (ReserveInventory req : request.getInventoryList()) {
                Optional<Inventory> availableInventoryOptional = inventoryRepository.findByProductId(req.getProductId()).blockOptional();
                if (availableInventoryOptional.isPresent() && availableInventoryOptional.get().getAvailableQuantity() >= req.getQuantity()) {
                    Inventory availableInventory = availableInventoryOptional.get();
                    availableInventory.setAvailableQuantity(availableInventory.getAvailableQuantity() - req.getQuantity());
                    availableInventory.setReservedQuantity(availableInventory.getReservedQuantity() + req.getQuantity());
                    inventoryRepository.save(availableInventory).subscribe();
                    var res = ReserveInventoryResponse.newBuilder()
                            .setProductId(req.getProductId())
                            .setOrderId(req.getOrderId())
                            .setSuccess(true)
                            .build();
                    response.addInvResponse(res);
                } else {
                    var res = ReserveInventoryResponse.newBuilder()
                            .setProductId(req.getProductId())
                            .setOrderId(req.getOrderId())
                            .setSuccess(false)
                            .build();
                    response.addInvResponse(res);
                }
            }

        } catch (Exception e) {
            responseObserver.onError(Status.UNKNOWN
                    .withDescription(Arrays.toString(e.getStackTrace()))
                    .asRuntimeException());
            return;
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
}