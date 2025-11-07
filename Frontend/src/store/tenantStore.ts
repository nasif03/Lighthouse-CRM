import { create } from 'zustand';
import { apiGet, apiPost } from '../utils/api';
import { useAuthStore } from './authStore';

export type Tenant = { id: string; name: string; domain?: string; createdAt?: string; updatedAt?: string };

type TenantState = {
  tenants: Tenant[];
  activeTenantId: string | null;
  isLoading: boolean;
  setActiveTenant: (id: string) => Promise<void>;
  fetchTenants: () => Promise<void>;
  addTenant: (tenant: Tenant) => void;
  refreshTenants: () => Promise<void>;
};

export const useTenantStore = create<TenantState>((set, get) => ({
  tenants: [],
  activeTenantId: null,
  isLoading: false,
  
  fetchTenants: async () => {
    const { token } = useAuthStore.getState();
    if (!token) {
      set({ tenants: [], activeTenantId: null });
      return;
    }

    set({ isLoading: true });
    try {
      // Use /api/tenants endpoint which returns user's organizations
      const data = await apiGet<{ tenants: Tenant[]; activeTenantId: string | null }>('/api/tenants', token);
      set({ 
        tenants: data.tenants || [],
        activeTenantId: data.activeTenantId || (data.tenants?.[0]?.id || null),
        isLoading: false
      });
    } catch (error) {
      console.error('Error fetching tenants:', error);
      set({ tenants: [], activeTenantId: null, isLoading: false });
    }
  },

  refreshTenants: async () => {
    await get().fetchTenants();
  },

  setActiveTenant: async (id: string) => {
    const { token } = useAuthStore.getState();
    if (!token) return;

    try {
      await apiPost('/api/tenants/switch', token, { tenant_id: id });
      set({ activeTenantId: id });
      // Clear cache to force refresh with new org context
      const { clearCache } = await import('../utils/api');
      clearCache();
      // Don't reload the page, just refresh the tenant list
      await get().fetchTenants();
    } catch (error: any) {
      console.error('Error switching tenant:', error);
      alert(error.message || 'Failed to switch organization');
      throw error;
    }
  },

  addTenant: (tenant) => set((s) => ({ tenants: [...s.tenants, tenant] })),
}));


