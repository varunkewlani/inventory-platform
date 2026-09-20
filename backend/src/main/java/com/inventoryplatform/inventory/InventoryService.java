package com.inventoryplatform.inventory;

import com.inventoryplatform.inventory.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    InventoryResponse adjust(AdjustInventoryRequest request);

    TransferResult transfer(TransferInventoryRequest request);

    InventoryResponse reserve(ReserveInventoryRequest request);

    InventoryResponse release(ReleaseInventoryRequest request);

    Page<InventoryResponse> list(Long warehouseId, Long productId, Pageable pageable);

    Page<InventoryMovementResponse> history(Long inventoryId, Pageable pageable);
}
