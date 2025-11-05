import { useTenantStore } from '../store/tenantStore';

export function TenantSwitcher() {
  const { tenants, activeTenantId, setActiveTenant } = useTenantStore();

  return (
    <div className="flex items-center gap-2">
      <span className="text-sm text-gray-700">Tenant</span>
      <select
        className="px-2 py-1.5 text-sm rounded border border-gray-200 bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-500"
        value={activeTenantId}
        onChange={(e) => setActiveTenant(e.target.value)}
      >
        {tenants.map((t) => (
          <option key={t.id} value={t.id}>{t.name}</option>
        ))}
      </select>
    </div>
  );
}


