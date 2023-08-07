package com.acuver.kpl.inventoryservice.repository;

import com.acuver.kpl.inventoryservice.model.Inventory;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends ReactiveMongoRepository<Inventory, String> {

    void findByProductId(String ProductId);

}