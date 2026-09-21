export interface InventoryRow {
  id: number;
  warehouseId: number;
  warehouseName: string;
  productId: number;
  productSku: string;
  productName: string;
  availableQuantity: number;
  reservedQuantity: number;
  updatedAt: string;
}
