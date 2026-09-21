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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Product-detail cache: key {@code product:{organizationId}:{id}}, TTL 5
 * minutes, invalidated (deleted, not updated in place) on every write —
 * simple cache-aside rather than write-through, since eviction is one line
 * and correctness doesn't depend on the cache ever holding a value. Cached
 * as a JSON string with explicit (de)serialization — see DashboardService's
 * javadoc for why, same reasoning applies here.
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Duration PRODUCT_CACHE_TTL = Duration.ofMinutes(5);

    private final ProductRepository productRepository;
    private final AuditService auditService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

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
        redisTemplate.delete(productCacheKey(organizationId, id));
        return after;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Long organizationId = TenantContext.getOrganizationId();
        String key = productCacheKey(organizationId, id);

        String cachedJson = redisTemplate.opsForValue().get(key);
        if (cachedJson != null) {
            return objectMapper.readValue(cachedJson, ProductResponse.class);
        }

        ProductResponse response = ProductResponse.from(findTenantScoped(id, organizationId));
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(response), PRODUCT_CACHE_TTL);
        return response;
    }

    private String productCacheKey(Long organizationId, Long id) {
        return "product:" + organizationId + ":" + id;
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
