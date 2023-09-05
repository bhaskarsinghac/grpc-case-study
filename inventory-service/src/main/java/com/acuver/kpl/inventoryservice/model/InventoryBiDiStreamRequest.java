package com.acuver.kpl.inventoryservice.model;

import com.acuver.kpl.inventory_components.InventoryAddResponse;
import com.acuver.kpl.inventory_components.InventoryAddStatus;
import com.acuver.kpl.inventory_components.InventoryRequest;
import com.acuver.kpl.inventoryservice.repository.BlockingRepository;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class InventoryBiDiStreamRequest implements StreamObserver<InventoryRequest> {

    final private BlockingRepository repository;
    final private StreamObserver<InventoryAddResponse> responseStreamObserver;


    public InventoryBiDiStreamRequest(BlockingRepository repository,
                                      StreamObserver<InventoryAddResponse> responseStreamObserver) {
        this.repository = repository;
        this.responseStreamObserver = responseStreamObserver;
    }

    @Override
    public void onNext(InventoryRequest request) {
        log.info("Got request : {}", request);

        responseStreamObserver
                .onNext(InventoryAddResponse
                        .newBuilder()
                        .setProductId(request.getProductId())
                        .setStatus(upsertInventory(request)).build());

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onCompleted() {
        log.info("All request completed");
        responseStreamObserver.onCompleted();
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
}
