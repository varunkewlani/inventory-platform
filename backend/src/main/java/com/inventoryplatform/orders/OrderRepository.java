package com.inventoryplatform.orders;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Order> findByOrganizationIdAndIdempotencyKey(Long organizationId, String idempotencyKey);

    long countByOrganizationIdAndStatus(Long organizationId, OrderStatus status);

    List<Order> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId, Pageable pageable);
}
