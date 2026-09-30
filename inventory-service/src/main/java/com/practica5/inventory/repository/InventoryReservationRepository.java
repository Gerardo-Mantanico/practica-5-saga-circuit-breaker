package com.practica5.inventory.repository;

import com.practica5.inventory.model.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    Optional<InventoryReservation> findByOrderId(Long orderId);
}
