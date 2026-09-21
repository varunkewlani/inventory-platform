export interface RecentOrderItem {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface RecentOrder {
  id: number;
  warehouseId: number;
  customerId: number;
  status: string;
  totalAmount: number;
  idempotencyKey: string | null;
  items: RecentOrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface DashboardData {
  totalProducts: number;
  totalWarehouses: number;
  availableInventory: number;
  pendingOrders: number;
  completedOrders: number;
  lowStockProducts: number;
  recentOrders: RecentOrder[];
}
