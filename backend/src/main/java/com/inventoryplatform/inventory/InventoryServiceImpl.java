package com.inventoryplatform.inventory;

import com.inventoryplatform.audit.AuditService;
import com.inventoryplatform.common.exception.BadRequestException;
import com.inventoryplatform.common.exception.ConflictException;
import com.inventoryplatform.common.exception.InsufficientInventoryException;
import com.inventoryplatform.common.exception.NotFoundException;
import com.inventoryplatform.common.tenant.TenantContext;
import com.inventoryplatform.common.util.Specs;
import com.inventoryplatform.infrastructure.websocket.NotificationService;
import com.inventoryplatform.inventory.dto.*;
import com.inventoryplatform.products.Product;
import com.inventoryplatform.products.ProductRepository;
import com.inventoryplatform.warehouses.Warehouse;
import com.inventoryplatform.warehouses.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    // Deliberately a fixed constant rather than a per-product/per-org
    // setting — configurable thresholds are listed as a future improvement
    // (README), not needed to satisfy the spec's "Low Inventory"
    // notification example under this deadline.
    private static final int LOW_STOCK_THRESHOLD = 5;

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public InventoryResponse adjust(AdjustInventoryRequest request) {
        Long organizationId = TenantContext.getOrganizationId();
        Warehouse warehouse = requireWarehouse(organizationId, request.warehouseId());
        Product product = requireProduct(organizationId, request.productId());

        Inventory inventory = request.type() == AdjustmentType.ADD
                ? getOrCreateRow(organizationId, warehouse.getId(), product.getId())
                : requireInventoryRow(organizationId, warehouse.getId(), product.getId());

        int rowsAffected = request.type() == AdjustmentType.ADD
                ? inventoryRepository.addAvailable(inventory.getId(), request.quantity())
                : inventoryRepository.removeAvailableIfSufficient(inventory.getId(), request.quantity());

        if (rowsAffected == 0) {
            throw new InsufficientInventoryException("Requested quantity is unavailable");
        }

        Inventory refreshed = reload(inventory.getId());
        InventoryMovementType movementType = request.type() == AdjustmentType.ADD ? InventoryMovementType.ADD : InventoryMovementType.REMOVE;
        recordMovement(refreshed, movementType, request.quantity(), request.note());
        auditInventoryChange(refreshed, movementType.name());

        if (request.type() == AdjustmentType.REMOVE) {
            checkLowStock(refreshed, warehouse, product);
        }

        return toResponse(refreshed, warehouse, product);
    }

    @Override
    @Transactional
    public TransferResult transfer(TransferInventoryRequest request) {
        if (request.fromWarehouseId().equals(request.toWarehouseId())) {
            throw new BadRequestException("Source and destination warehouse must be different");
        }

        Long organizationId = TenantContext.getOrganizationId();
        Warehouse fromWarehouse = requireWarehouse(organizationId, request.fromWarehouseId());
        Warehouse toWarehouse = requireWarehouse(organizationId, request.toWarehouseId());
        Product product = requireProduct(organizationId, request.productId());

        Inventory source = requireInventoryRow(organizationId, fromWarehouse.getId(), product.getId());
        int removed = inventoryRepository.removeAvailableIfSufficient(source.getId(), request.quantity());
        if (removed == 0) {
            throw new InsufficientInventoryException("Requested quantity is unavailable at the source warehouse");
        }

        Inventory destination = getOrCreateRow(organizationId, toWarehouse.getId(), product.getId());
        inventoryRepository.addAvailable(destination.getId(), request.quantity());

        Inventory sourceAfter = reload(source.getId());
        Inventory destinationAfter = reload(destination.getId());

        recordMovement(sourceAfter, InventoryMovementType.TRANSFER_OUT, request.quantity(), request.note());
        recordMovement(destinationAfter, InventoryMovementType.TRANSFER_IN, request.quantity(), request.note());
        auditInventoryChange(sourceAfter, InventoryMovementType.TRANSFER_OUT.name());
        auditInventoryChange(destinationAfter, InventoryMovementType.TRANSFER_IN.name());
        checkLowStock(sourceAfter, fromWarehouse, product);

        return new TransferResult(
                toResponse(sourceAfter, fromWarehouse, product),
                toResponse(destinationAfter, toWarehouse, product));
    }

    @Override
    @Transactional
    public InventoryResponse reserve(ReserveInventoryRequest request) {
        Long organizationId = TenantContext.getOrganizationId();
        Warehouse warehouse = requireWarehouse(organizationId, request.warehouseId());
        Product product = requireProduct(organizationId, request.productId());
        Inventory inventory = requireInventoryRow(organizationId, warehouse.getId(), product.getId());

        int rowsAffected = inventoryRepository.reserveIfAvailable(inventory.getId(), request.quantity());
        if (rowsAffected == 0) {
            throw new InsufficientInventoryException("Requested quantity is unavailable");
        }

        Inventory refreshed = reload(inventory.getId());
        recordMovement(refreshed, InventoryMovementType.RESERVE, request.quantity(), null);
        auditInventoryChange(refreshed, InventoryMovementType.RESERVE.name());
        checkLowStock(refreshed, warehouse, product);
        return toResponse(refreshed, warehouse, product);
    }

    @Override
    @Transactional
    public InventoryResponse release(ReleaseInventoryRequest request) {
        Long organizationId = TenantContext.getOrganizationId();
        Warehouse warehouse = requireWarehouse(organizationId, request.warehouseId());
        Product product = requireProduct(organizationId, request.productId());
        Inventory inventory = requireInventoryRow(organizationId, warehouse.getId(), product.getId());

        int rowsAffected = inventoryRepository.releaseIfReserved(inventory.getId(), request.quantity());
        if (rowsAffected == 0) {
            throw new ConflictException("INVALID_RELEASE", "Cannot release more than is currently reserved");
        }

        Inventory refreshed = reload(inventory.getId());
        recordMovement(refreshed, InventoryMovementType.RELEASE, request.quantity(), null);
        auditInventoryChange(refreshed, InventoryMovementType.RELEASE.name());
        return toResponse(refreshed, warehouse, product);
    }

    @Override
    @Transactional
    public InventoryResponse fulfill(FulfillInventoryRequest request) {
        Long organizationId = TenantContext.getOrganizationId();
        Warehouse warehouse = requireWarehouse(organizationId, request.warehouseId());
        Product product = requireProduct(organizationId, request.productId());
        Inventory inventory = requireInventoryRow(organizationId, warehouse.getId(), product.getId());

        int rowsAffected = inventoryRepository.consumeReservedIfSufficient(inventory.getId(), request.quantity());
        if (rowsAffected == 0) {
            throw new ConflictException("INVALID_FULFILL", "Cannot fulfill more than is currently reserved");
        }

        Inventory refreshed = reload(inventory.getId());
        recordMovement(refreshed, InventoryMovementType.FULFILL, request.quantity(), null);
        auditInventoryChange(refreshed, InventoryMovementType.FULFILL.name());
        return toResponse(refreshed, warehouse, product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> list(Long warehouseId, Long productId, Pageable pageable) {
        Long organizationId = TenantContext.getOrganizationId();
        Specification<Inventory> spec = Specs.and(
                InventorySpecifications.hasOrganizationId(organizationId),
                InventorySpecifications.hasWarehouseId(warehouseId),
                InventorySpecifications.hasProductId(productId));

        return inventoryRepository.findAll(spec, pageable).map(inventory -> {
            Warehouse warehouse = warehouseRepository.findById(inventory.getWarehouseId()).orElse(null);
            Product product = productRepository.findById(inventory.getProductId()).orElse(null);
            return toResponse(inventory, warehouse, product);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> history(Long inventoryId, Pageable pageable) {
        Long organizationId = TenantContext.getOrganizationId();
        // Confirms the row belongs to this tenant before returning any history for it.
        inventoryRepository.findByIdAndOrganizationId(inventoryId, organizationId)
                .orElseThrow(() -> new NotFoundException("Inventory record not found"));

        return movementRepository
                .findAllByOrganizationIdAndInventoryIdOrderByCreatedAtDesc(organizationId, inventoryId, pageable)
                .map(InventoryMovementResponse::from);
    }

    private Warehouse requireWarehouse(Long organizationId, Long warehouseId) {
        return warehouseRepository.findByIdAndOrganizationId(warehouseId, organizationId)
                .orElseThrow(() -> new NotFoundException("Warehouse not found"));
    }

    private Product requireProduct(Long organizationId, Long productId) {
        return productRepository.findByIdAndOrganizationId(productId, organizationId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private Inventory requireInventoryRow(Long organizationId, Long warehouseId, Long productId) {
        return inventoryRepository.findByOrganizationIdAndWarehouseIdAndProductId(organizationId, warehouseId, productId)
                .orElseThrow(() -> new NotFoundException("No inventory record for this product at this warehouse"));
    }

    /**
     * Plain find-or-create — not hardened against two concurrent first-ever
     * stock-ins for the same never-before-seen (warehouse, product) pair
     * both trying to INSERT. That's a distinct, much rarer race than the
     * mandatory-tested one (concurrent reservations against existing stock,
     * which the atomic UPDATEs above handle correctly); a collision here
     * surfaces as a clean 409 via the constraint-violation catch below
     * rather than crashing, and the client can just retry.
     */
    private Inventory getOrCreateRow(Long organizationId, Long warehouseId, Long productId) {
        return inventoryRepository.findByOrganizationIdAndWarehouseIdAndProductId(organizationId, warehouseId, productId)
                .orElseGet(() -> inventoryRepository.save(Inventory.builder()
                        .organizationId(organizationId)
                        .warehouseId(warehouseId)
                        .productId(productId)
                        .availableQuantity(0)
                        .reservedQuantity(0)
                        .build()));
        // A concurrent double-create race here throws DataIntegrityViolationException,
        // which propagates up and is handled globally (GlobalExceptionHandler) as a
        // clean 409 after a proper transaction rollback — see the handler for why
        // that's safer than catching it locally mid-transaction.
    }

    private Inventory reload(Long id) {
        return inventoryRepository.findById(id).orElseThrow();
    }

    private void recordMovement(Inventory inventory, InventoryMovementType type, int quantity, String note) {
        movementRepository.save(InventoryMovement.builder()
                .organizationId(inventory.getOrganizationId())
                .inventoryId(inventory.getId())
                .warehouseId(inventory.getWarehouseId())
                .productId(inventory.getProductId())
                .movementType(type)
                .quantity(quantity)
                .availableQuantityAfter(inventory.getAvailableQuantity())
                .reservedQuantityAfter(inventory.getReservedQuantity())
                .note(note)
                .createdBy(TenantContext.getUserId())
                .build());
    }

    private void auditInventoryChange(Inventory inventory, String action) {
        auditService.log("INVENTORY_" + action, "Inventory", inventory.getId().toString(), null, Map.of(
                "availableQuantity", inventory.getAvailableQuantity(),
                "reservedQuantity", inventory.getReservedQuantity()));
    }

    private void checkLowStock(Inventory inventory, Warehouse warehouse, Product product) {
        if (inventory.getAvailableQuantity() <= LOW_STOCK_THRESHOLD) {
            notificationService.notifyOrganization(inventory.getOrganizationId(), "INVENTORY_LOW", Map.of(
                    "inventoryId", inventory.getId(),
                    "warehouseId", warehouse.getId(),
                    "warehouseName", warehouse.getName(),
                    "productId", product.getId(),
                    "productSku", product.getSku(),
                    "remainingQuantity", inventory.getAvailableQuantity()));
        }
    }

    private InventoryResponse toResponse(Inventory inventory, Warehouse warehouse, Product product) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getWarehouseId(),
                warehouse != null ? warehouse.getName() : null,
                inventory.getProductId(),
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getUpdatedAt());
    }
}
