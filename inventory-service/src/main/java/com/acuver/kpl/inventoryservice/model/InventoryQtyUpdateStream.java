package com.acuver.kpl.inventoryservice.model;

import com.acuver.kpl.inventory_components.AddQtyRequest;
import com.acuver.kpl.inventory_components.AddQtyResponse;
import com.acuver.kpl.inventoryservice.repository.BlockingRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class InventoryQtyUpdateStream implements StreamObserver<AddQtyRequest> {

    private final StreamObserver<AddQtyResponse> responseObserver;
    private final BlockingRepository repository;

    private String productId;

    public InventoryQtyUpdateStream(StreamObserver<AddQtyResponse> responseObserver, BlockingRepository repository) {
        this.responseObserver = responseObserver;
        this.repository = repository;
    }

    @Override
    public void onNext(AddQtyRequest addQtyRequest) {
        this.productId = addQtyRequest.getProductId();
        Optional<Inventory> byId = repository.findById(addQtyRequest.getProductId());
        if(byId.isPresent()){
            Inventory inventory = byId.get();
            inventory.setAvailableQuantity(inventory.getAvailableQuantity()+ addQtyRequest.getQty());
            repository.save(inventory);
        }else {
            log.error("Given productId not exists {}", addQtyRequest.getProductId());
            this.responseObserver.onError(Status.NOT_FOUND.asException());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        this.responseObserver.onError(throwable);
    }

    @Override
    public void onCompleted() {

        Integer availableQuantity = repository.findByProductId(productId).getAvailableQuantity();
        log.info("Current available quantity for {} is {}", productId, availableQuantity);

        AddQtyResponse response = AddQtyResponse.newBuilder()
                .setMessage("Current available quantity : "+availableQuantity).build();
        this.responseObserver.onNext(response);
        this.responseObserver.onCompleted();
    }
}
