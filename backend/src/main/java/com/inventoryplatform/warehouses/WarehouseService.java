package com.inventoryplatform.warehouses;

import com.inventoryplatform.warehouses.dto.CreateWarehouseRequest;
import com.inventoryplatform.warehouses.dto.UpdateWarehouseRequest;
import com.inventoryplatform.warehouses.dto.WarehouseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WarehouseService {

    WarehouseResponse create(CreateWarehouseRequest request);

    WarehouseResponse update(Long id, UpdateWarehouseRequest request);

    WarehouseResponse getById(Long id);

    Page<WarehouseResponse> list(Pageable pageable);
}
