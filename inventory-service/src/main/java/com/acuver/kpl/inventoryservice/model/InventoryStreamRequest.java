package com.acuver.kpl.inventoryservice.model;

import com.acuver.kpl.inventory_components.InventoryAddResponse;
import com.acuver.kpl.inventory_components.InventoryAddStatus;
import com.acuver.kpl.inventory_components.InventoryRequest;
import com.acuver.kpl.inventoryservice.repository.InventoryRepository;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InventoryStreamRequest implements StreamObserver<InventoryRequest> {

    final private InventoryRepository inventoryRepository;
    final private StreamObserver<InventoryAddResponse> responseStreamObserver;

    public InventoryStreamRequest(InventoryRepository inventoryRepository,
                                  StreamObserver<InventoryAddResponse> responseStreamObserver) {
        this.inventoryRepository = inventoryRepository;
        this.responseStreamObserver = responseStreamObserver;
    }

    @Override
    public void onNext(InventoryRequest request) {
        log.info("Got request : {}", request);
        Inventory inventory = new Inventory();
        inventory.setProductId(request.getProductId());
        inventory.setProductName(request.getProductName());
        inventory.setAvailableQuantity(request.getQty());

        inventoryRepository.save(inventory)
                .subscribe(inventory1 -> log.info("Saved {}",inventory1.getProductId()));

    }

    @Override
    public void onError(Throwable throwable) {
        this.responseStreamObserver.onError(throwable);
    }

    @Override
    public void onCompleted() {
        InventoryAddResponse response = InventoryAddResponse.newBuilder()
                .setStatus(InventoryAddStatus.SUCCESS).build();
        this.responseStreamObserver.onNext(response);
        this.responseStreamObserver.onCompleted();
    }
}
