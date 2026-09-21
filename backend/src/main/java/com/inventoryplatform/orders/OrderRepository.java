package com.inventoryplatform.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Order> findByOrganizationIdAndIdempotencyKey(Long organizationId, String idempotencyKey);
}
