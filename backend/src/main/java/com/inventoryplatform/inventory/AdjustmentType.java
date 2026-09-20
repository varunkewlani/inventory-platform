package com.inventoryplatform.inventory;

/** What a client is allowed to ask for via POST /inventory/adjust — a
 *  deliberately narrower set than {@link InventoryMovementType}, which also
 *  has entries (TRANSFER_*, RESERVE, RELEASE) that only ever get created by
 *  other operations, never requested directly. */
public enum AdjustmentType {
    ADD,
    REMOVE
}
