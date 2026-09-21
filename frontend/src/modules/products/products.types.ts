export interface Product {
  id: number;
  sku: string;
  name: string;
  description: string | null;
  price: number;
  status: "ACTIVE" | "DISABLED";
  createdAt: string;
  updatedAt: string;
}
