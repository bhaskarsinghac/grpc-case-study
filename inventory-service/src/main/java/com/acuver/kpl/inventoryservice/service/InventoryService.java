package com.acuver.kpl.inventoryservice.service;

import com.acuver.kpl.inventory_components.*;
import com.acuver.kpl.inventoryservice.model.Inventory;
import com.acuver.kpl.inventoryservice.model.InventoryBiDiStreamRequest;
import com.acuver.kpl.inventoryservice.model.InventoryStreamRequest;
import com.acuver.kpl.inventoryservice.repository.InventoryRepository;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
public class InventoryService extends InventoryServiceGrpc.InventoryServiceImplBase {

    final private InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }


    @Override
    public void addInventory(InventoryRequest request, StreamObserver<InventoryAddResponse> responseObserver) {
        log.info("Got request : {}", request);
        Inventory inventory = new Inventory();
        inventory.setProductId(request.getProductId());
        inventory.setProductName(request.getProductName());
        inventory.setAvailableQuantity(request.getQty());

        inventoryRepository.save(inventory)
                .subscribe();

        InventoryAddResponse response = InventoryAddResponse
                .newBuilder().setStatus(InventoryAddStatus.SUCCESS).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public StreamObserver<InventoryRequest> addMultipleInventory(StreamObserver<InventoryAddResponse> responseObserver) {
        return new InventoryStreamRequest(inventoryRepository, responseObserver);
    }

    @Override
    public StreamObserver<InventoryRequest> addStreamInventoryUpdate(StreamObserver<InventoryAddResponse> responseObserver) {
        return new InventoryBiDiStreamRequest(inventoryRepository, responseObserver);
    }

    @Override
    public void fetchInventory(InventoryFetch request, StreamObserver<InventoryResponse> responseObserver) {
        request.getIdsList()
                .stream().forEach(id -> {
                    log.info("id : {}", id);
                    Inventory inventory = inventoryRepository.findByProductId(id).block();
                    InventoryResponse inventoryResponse
                            = InventoryResponse.newBuilder()
                            .setProductId(inventory.getProductId())
                            .setProductDesc(inventory.getProductName())
                            .setAvailableQty(inventory.getAvailableQuantity())
                            //.setReservedQty(Optional.of(inventory.getReservedQuantity()).get())
                            .build();
                    responseObserver.onNext(inventoryResponse);

                });

        responseObserver.onCompleted();
    }
}