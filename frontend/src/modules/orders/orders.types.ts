export interface OrderItem {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export type OrderStatus = "PENDING" | "CONFIRMED" | "PROCESSING" | "COMPLETED" | "CANCELLED";

export interface Order {
  id: number;
  warehouseId: number;
  customerId: number;
  status: OrderStatus;
  totalAmount: number;
  idempotencyKey: string | null;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export const NEXT_STATUS: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ["CONFIRMED", "CANCELLED"],
  CONFIRMED: ["PROCESSING", "CANCELLED"],
  PROCESSING: ["COMPLETED", "CANCELLED"],
  COMPLETED: [],
  CANCELLED: [],
};
