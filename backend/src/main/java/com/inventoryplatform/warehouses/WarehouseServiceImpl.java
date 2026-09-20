package com.inventoryplatform.warehouses;

import com.inventoryplatform.common.exception.NotFoundException;
import com.inventoryplatform.common.tenant.TenantContext;
import com.inventoryplatform.warehouses.dto.CreateWarehouseRequest;
import com.inventoryplatform.warehouses.dto.UpdateWarehouseRequest;
import com.inventoryplatform.warehouses.dto.WarehouseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public WarehouseResponse create(CreateWarehouseRequest request) {
        Warehouse warehouse = Warehouse.builder()
                .organizationId(TenantContext.getOrganizationId())
                .name(request.name())
                .address(request.address())
                .status(WarehouseStatus.ACTIVE)
                .build();

        return WarehouseResponse.from(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional
    public WarehouseResponse update(Long id, UpdateWarehouseRequest request) {
        Warehouse warehouse = findTenantScoped(id);

        if (request.name() != null) {
            warehouse.setName(request.name());
        }
        if (request.address() != null) {
            warehouse.setAddress(request.address());
        }
        if (request.status() != null) {
            warehouse.setStatus(request.status());
        }

        return WarehouseResponse.from(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getById(Long id) {
        return WarehouseResponse.from(findTenantScoped(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WarehouseResponse> list(Pageable pageable) {
        return warehouseRepository.findAllByOrganizationId(TenantContext.getOrganizationId(), pageable)
                .map(WarehouseResponse::from);
    }

    private Warehouse findTenantScoped(Long id) {
        return warehouseRepository.findByIdAndOrganizationId(id, TenantContext.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Warehouse not found"));
    }
}
