package com.acuver.kpl.inventoryservice.service;

import com.acuver.kpl.inventory_components.ReserveInventory;
import com.acuver.kpl.inventory_components.ReserveInventoryListResponse;
import com.acuver.kpl.inventory_components.ReserveInventoryRequest;
import com.acuver.kpl.inventory_components.ReserveInventoryResponse;
import com.acuver.kpl.inventoryservice.model.Inventory;
import com.acuver.kpl.inventoryservice.repository.InventoryRepository;
import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

public class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testReserveInventory_Success() {
        // Create a sample request
        ReserveInventoryRequest request = ReserveInventoryRequest.newBuilder()
                .addInventory(ReserveInventory.newBuilder()
                        .setProductId("prod123")
                        .setOrderId("ord456")
                        .setQuantity(5)
                        .build())
                .build();

        // Create a sample inventory
        Inventory sampleInventory = new Inventory();
        sampleInventory.setProductId("prod123");
        sampleInventory.setAvailableQuantity(10);
        sampleInventory.setReservedQuantity(0);

        // Mock the behavior of findByProductId
        when(inventoryRepository.findByProductId(any())).thenReturn(Mono.just(sampleInventory));
        when(inventoryRepository.save(any())).thenReturn(Mono.just(sampleInventory));

        // Create a mock StreamObserver
        StreamObserver<ReserveInventoryListResponse> responseObserver = mock(StreamObserver.class);

        // Call the reserveInventory method
        inventoryService.reserveInventory(request, responseObserver);

        // Verify that the response contains a success entry
        verify(responseObserver).onNext(argThat(response ->
                response.getInvResponseList().stream()
                        .anyMatch(ReserveInventoryResponse::getSuccess)
        ));
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        // Verify that the inventoryRepository.save was called
        verify(inventoryRepository).save(sampleInventory);
    }

    @Test
    public void testReserveInventory_Failure() {
        // Create a sample request
        ReserveInventoryRequest request = ReserveInventoryRequest.newBuilder()
                .addInventory(ReserveInventory.newBuilder()
                        .setProductId("prod123")
                        .setOrderId("ord456")
                        .setQuantity(5)
                        .build())
                .build();

        // Mock the behavior of findByProductId to return an empty result
        when(inventoryRepository.findByProductId(any())).thenReturn(Mono.empty());
        when(inventoryRepository.save(any())).thenReturn(Mono.empty());

        // Create a mock StreamObserver
        StreamObserver<ReserveInventoryListResponse> responseObserver = mock(StreamObserver.class);

        // Call the reserveInventory method
        inventoryService.reserveInventory(request, responseObserver);

        // Verify that the response contains a failure entry
        verify(responseObserver).onNext(argThat(response ->
                response.getInvResponseList().stream()
                        .anyMatch(responseItem -> !responseItem.getSuccess())
        ));
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        // Verify that the inventoryRepository.save was not called
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }
}
