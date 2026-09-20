package com.inventoryplatform.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    Page<InventoryMovement> findAllByOrganizationIdAndInventoryIdOrderByCreatedAtDesc(
            Long organizationId, Long inventoryId, Pageable pageable);
}
