export interface Warehouse {
  id: number;
  name: string;
  address: string | null;
  status: "ACTIVE" | "DISABLED";
  createdAt: string;
  updatedAt: string;
}
