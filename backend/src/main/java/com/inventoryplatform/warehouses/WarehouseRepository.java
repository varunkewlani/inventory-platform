package com.inventoryplatform.warehouses;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByIdAndOrganizationId(Long id, Long organizationId);

    Page<Warehouse> findAllByOrganizationId(Long organizationId, Pageable pageable);

    List<Warehouse> findAllByOrganizationId(Long organizationId);

    long countByOrganizationId(Long organizationId);
}
