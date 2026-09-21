package com.inventoryplatform.products;

import com.inventoryplatform.audit.AuditService;
import com.inventoryplatform.common.exception.ConflictException;
import com.inventoryplatform.common.exception.NotFoundException;
import com.inventoryplatform.common.tenant.TenantContext;
import com.inventoryplatform.common.util.Specs;
import com.inventoryplatform.products.dto.CreateProductRequest;
import com.inventoryplatform.products.dto.ProductResponse;
import com.inventoryplatform.products.dto.UpdateProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Long organizationId = TenantContext.getOrganizationId();
        if (productRepository.existsByOrganizationIdAndSku(organizationId, request.sku())) {
            throw new ConflictException("SKU_TAKEN", "A product with this SKU already exists");
        }

        Product product = Product.builder()
                .organizationId(organizationId)
                .sku(request.sku())
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .status(ProductStatus.ACTIVE)
                .build();

        ProductResponse response = ProductResponse.from(productRepository.save(product));
        auditService.log("PRODUCT_CREATED", "Product", response.id().toString(), null, response);
        return response;
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Long organizationId = TenantContext.getOrganizationId();
        Product product = findTenantScoped(id, organizationId);
        ProductResponse before = ProductResponse.from(product);

        if (request.sku() != null && !request.sku().equals(product.getSku())) {
            if (productRepository.existsByOrganizationIdAndSkuAndIdNot(organizationId, request.sku(), id)) {
                throw new ConflictException("SKU_TAKEN", "A product with this SKU already exists");
            }
            product.setSku(request.sku());
        }
        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.status() != null) {
            product.setStatus(request.status());
        }

        ProductResponse after = ProductResponse.from(productRepository.save(product));
        auditService.log("PRODUCT_UPDATED", "Product", id.toString(), before, after);
        return after;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductResponse.from(findTenantScoped(id, TenantContext.getOrganizationId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String search, ProductStatus status, Pageable pageable) {
        Specification<Product> spec = Specs.and(
                ProductSpecifications.hasOrganizationId(TenantContext.getOrganizationId()),
                ProductSpecifications.search(search),
                ProductSpecifications.hasStatus(status));

        return productRepository.findAll(spec, pageable).map(ProductResponse::from);
    }

    private Product findTenantScoped(Long id, Long organizationId) {
        return productRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }
}
