package com.acuver.kpl.inventoryservice.repository;

import com.acuver.kpl.inventoryservice.model.Inventory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockingRepository extends MongoRepository<Inventory, String> {

    Inventory findByProductId(String productId);

}