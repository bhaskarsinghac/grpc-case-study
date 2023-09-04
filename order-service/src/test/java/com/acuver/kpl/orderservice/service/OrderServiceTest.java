package com.acuver.kpl.orderservice.service;

import com.acuver.kpl.inventory_components.InventoryServiceGrpc;
import com.acuver.kpl.inventory_components.ReserveInventoryListResponse;
import com.acuver.kpl.inventory_components.ReserveInventoryRequest;
import com.acuver.kpl.orderservice.model.Order;
import com.acuver.kpl.orderservice.repository.OrderRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryServiceGrpc.InventoryServiceFutureStub futureStub;

    @InjectMocks
    private OrderService orderService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateOrder_Success() {
        // Arrange
        Order createOrderReq = new Order(); // Create a sample order

        // Mock the behavior of orderRepository.save
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(createOrderReq));

        // Mock the behavior of futureStub.reserveInventory
        ReserveInventoryListResponse response = ReserveInventoryListResponse.newBuilder().build(); // Create a sample response
        ListenableFuture<ReserveInventoryListResponse> future = Futures.immediateFuture(response);
        when(futureStub.reserveInventory(any(ReserveInventoryRequest.class))).thenReturn(future);

        // Act
        Mono<Object> result = orderService.createOrder(createOrderReq);

        // Assert  TODO: Work in progress
/*        StepVerifier.create(result)
                .expectNextMatches(obj -> {
                    // Add assertions for the expected outcome
                    return Boolean.TRUE;
                })
                .verifyComplete();*/
    }
}
