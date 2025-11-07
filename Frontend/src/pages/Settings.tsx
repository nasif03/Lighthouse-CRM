import Input from '../components/ui/Input';
import Button from '../components/ui/Button';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import { useTenantStore } from '../store/tenantStore';
import { useAuthStore } from '../store/authStore';
import { useState, useEffect } from 'react';
import { apiGet, apiPost, apiPut } from '../utils/api';

type Organization = {
  id: string;
  name: string;
  domain: string;
  createdAt: string;
  updatedAt: string;
};

export default function Settings() {
  const { token } = useAuthStore();
  const { tenants, activeTenantId, fetchTenants, refreshTenants } = useTenantStore();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [newOrgName, setNewOrgName] = useState('');
  const [editingOrgId, setEditingOrgId] = useState<string | null>(null);
  const [editOrgName, setEditOrgName] = useState('');

  const activeOrg = organizations.find(org => org.id === activeTenantId);

  useEffect(() => {
    loadOrganizations();
  }, [token]);

  const loadOrganizations = async () => {
    if (!token) return;
    setIsLoading(true);
    try {
      const data = await apiGet<Organization[]>('/api/organizations', token);
      setOrganizations(data);
    } catch (error) {
      console.error('Error loading organizations:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateOrganization = async () => {
    if (!token || !newOrgName.trim()) return;
    setIsCreating(true);
    try {
      const data = await apiPost<Organization>('/api/organizations', token, {
        name: newOrgName.trim(),
      });
      setOrganizations([...organizations, data]);
      setNewOrgName('');
      // Refresh tenant list
      await refreshTenants();
    } catch (error: any) {
      alert(error.message || 'Failed to create organization');
    } finally {
      setIsCreating(false);
    }
  };

  const handleUpdateOrganization = async (orgId: string) => {
    if (!token || !editOrgName.trim()) return;
    try {
      const data = await apiPut<Organization>(`/api/organizations/${orgId}`, token, {
        name: editOrgName.trim(),
      });
      setOrganizations(organizations.map(org => org.id === orgId ? data : org));
      setEditingOrgId(null);
      setEditOrgName('');
      // Refresh tenant list
      await refreshTenants();
    } catch (error: any) {
      alert(error.message || 'Failed to update organization');
    }
  };

  const startEditing = (org: Organization) => {
    setEditingOrgId(org.id);
    setEditOrgName(org.name);
  };

  const cancelEditing = () => {
    setEditingOrgId(null);
    setEditOrgName('');
  };

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>Current Organization</CardHeader>
        <CardContent>
          {activeOrg ? (
            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-gray-700 mb-1 block">
                  Organization Name
                </label>
                {editingOrgId === activeOrg.id ? (
                  <div className="flex gap-2">
                    <Input
                      value={editOrgName}
                      onChange={(e) => setEditOrgName(e.target.value)}
                      className="flex-1"
                    />
                    <Button
                      onClick={() => handleUpdateOrganization(activeOrg.id)}
                      disabled={!editOrgName.trim()}
                    >
                      Save
                    </Button>
                    <Button variant="secondary" onClick={cancelEditing}>
                      Cancel
                    </Button>
                  </div>
                ) : (
                  <div className="flex items-center justify-between">
                    <span className="text-gray-900">{activeOrg.name}</span>
                    <Button variant="secondary" onClick={() => startEditing(activeOrg)}>
                      Edit
                    </Button>
                  </div>
                )}
              </div>
              <div>
                <label className="text-sm font-medium text-gray-700 mb-1 block">
                  Domain
                </label>
                <span className="text-gray-600">{activeOrg.domain}</span>
              </div>
            </div>
          ) : (
            <p className="text-gray-600">No active organization</p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>All Organizations</CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex gap-2 mb-4">
              <Input
                placeholder="Enter organization name"
                value={newOrgName}
                onChange={(e) => setNewOrgName(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleCreateOrganization()}
                className="flex-1"
              />
              <Button
                onClick={handleCreateOrganization}
                disabled={!newOrgName.trim() || isCreating}
              >
                {isCreating ? 'Creating...' : 'Create Organization'}
              </Button>
            </div>

            {isLoading ? (
              <p className="text-gray-600">Loading organizations...</p>
            ) : organizations.length === 0 ? (
              <p className="text-gray-600">No organizations found. Create one above.</p>
            ) : (
              <div className="space-y-2">
                {organizations.map((org) => (
                  <div
                    key={org.id}
                    className="flex items-center justify-between p-3 border border-gray-200 rounded-lg"
                  >
                    <div className="flex-1">
                      {editingOrgId === org.id ? (
                        <div className="flex gap-2">
                          <Input
                            value={editOrgName}
                            onChange={(e) => setEditOrgName(e.target.value)}
                            className="flex-1"
                          />
                          <Button
                            onClick={() => handleUpdateOrganization(org.id)}
                            disabled={!editOrgName.trim()}
                            size="sm"
                          >
                            Save
                          </Button>
                          <Button variant="secondary" onClick={cancelEditing} size="sm">
                            Cancel
                          </Button>
                        </div>
                      ) : (
                        <div>
                          <div className="font-medium text-gray-900">{org.name}</div>
                          <div className="text-sm text-gray-500">{org.domain}</div>
                        </div>
                      )}
                    </div>
                    {editingOrgId !== org.id && (
                      <Button
                        variant="secondary"
                        onClick={() => startEditing(org)}
                        size="sm"
                      >
                        Edit
                      </Button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
