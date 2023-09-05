package com.acuver.kpl.inventoryservice.service;

import com.acuver.kpl.inventory_components.*;
import com.acuver.kpl.inventoryservice.model.Inventory;
import com.acuver.kpl.inventoryservice.model.InventoryBiDiStreamRequest;
import com.acuver.kpl.inventoryservice.model.InventoryQtyUpdateStream;
import com.acuver.kpl.inventoryservice.repository.BlockingRepository;
import com.acuver.kpl.inventoryservice.repository.InventoryRepository;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@GrpcService
public class InventoryService extends InventoryServiceGrpc.InventoryServiceImplBase {

    final private InventoryRepository inventoryRepository;
    final private BlockingRepository repository;

    @Autowired
    public InventoryService(InventoryRepository inventoryRepository, BlockingRepository repository) {
        this.inventoryRepository = inventoryRepository;
        this.repository = repository;
    }


    @Override
    public void addInventory(InventoryRequest request, StreamObserver<InventoryAddResponse> responseObserver) {
        log.info("Got request : {}", request);

        InventoryAddStatus addStatus = upsertInventory(request);

        var response = InventoryAddResponse
                .newBuilder()
                .setProductId(request.getProductId())
                .setStatus(addStatus)
                .build();


        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    private InventoryAddStatus upsertInventory(InventoryRequest request) {
        Optional<Inventory> byId = repository.findById(request.getProductId());
        if (byId.isPresent()) {
            Inventory existingInv = byId.get();
            existingInv.setAvailableQuantity(request.getQty());
            repository.save(existingInv);
            return InventoryAddStatus.UPDATED;

        } else {
            Inventory inventory = new Inventory();
            inventory.setProductId(request.getProductId());
            inventory.setProductName(request.getProductName());
            inventory.setAvailableQuantity(request.getQty());
            inventory.setReservedQuantity(0);
            repository.save(inventory);
            return InventoryAddStatus.CREATED;
        }
    }

    @Override
    public StreamObserver<InventoryRequest> addStreamInventoryUpdate(StreamObserver<InventoryAddResponse> responseObserver) {
        return new InventoryBiDiStreamRequest(repository, responseObserver);
    }

    @Override
    public void fetchInventory(InventoryFetch request, StreamObserver<InventoryFetchResponse> responseObserver) {
        request.getIdsList()
                .stream().forEach(id -> {
                    log.info("id : {}", id);
                    var response = InventoryFetchResponse.newBuilder();
                    Inventory inventory = repository.findByProductId(id); // TODO: 9/5/2023 implement reactive way or use non reactive repo
                    if (Objects.nonNull(inventory)) {
                        InventoryResponse details = InventoryResponse.newBuilder()
                                .setProductId(inventory.getProductId())
                                .setProductDesc(inventory.getProductName())
                                .setAvailableQty(inventory.getAvailableQuantity())
                                .setReservedQty(Optional.ofNullable(inventory.getReservedQuantity()).orElse(0))
                                .build();
                        response.setDetails(details);
                    } else {
                        NotFoundResponse notFoundResponse = NotFoundResponse
                                .newBuilder().setMessage("Product Id NOT FOUND : "+id)
                                .build();
                        response.setError(notFoundResponse);
                    }
                    responseObserver.onNext(response.build());
                });

        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<AddQtyRequest> updateQty(StreamObserver<AddQtyResponse> responseObserver) {
        return new InventoryQtyUpdateStream(responseObserver, repository);
    }
}