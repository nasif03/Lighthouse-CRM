import { create } from 'zustand';

export type Tenant = { id: string; name: string };

type TenantState = {
  tenants: Tenant[];
  activeTenantId: string;
  setActiveTenant: (id: string) => void;
  addTenant: (tenant: Tenant) => void;
};

export const useTenantStore = create<TenantState>((set) => ({
  tenants: [
    { id: 'acme', name: 'Acme Inc' },
    { id: 'globex', name: 'Globex Co' },
  ],
  activeTenantId: 'acme',
  setActiveTenant: (id) => set({ activeTenantId: id }),
  addTenant: (tenant) => set((s) => ({ tenants: [...s.tenants, tenant] })),
}));


