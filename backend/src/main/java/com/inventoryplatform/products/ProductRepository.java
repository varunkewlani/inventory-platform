package com.inventoryplatform.products;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByOrganizationIdAndSku(Long organizationId, String sku);

    boolean existsByOrganizationIdAndSkuAndIdNot(Long organizationId, String sku, Long id);

    long countByOrganizationId(Long organizationId);
}
