package com.practica5.inventory.repository;

import com.practica5.inventory.model.InventoryProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryProductRepository extends JpaRepository<InventoryProduct, Long> {
    Optional<InventoryProduct> findByProductCode(String productCode);
}
