package com.inventoryplatform.customers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdAndOrganizationId(Long id, Long organizationId);

    Page<Customer> findAllByOrganizationId(Long organizationId, Pageable pageable);
}
