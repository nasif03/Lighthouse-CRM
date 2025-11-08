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
  const { token, user } = useAuthStore();
  const { tenants, activeTenantId, fetchTenants, refreshTenants, setActiveTenant } = useTenantStore();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [newOrgName, setNewOrgName] = useState('');
  const [editingOrgId, setEditingOrgId] = useState<string | null>(null);
  const [editOrgName, setEditOrgName] = useState('');
  const [joinOrgName, setJoinOrgName] = useState('');
  const [joinEmail, setJoinEmail] = useState(user?.email || '');
  const [isJoining, setIsJoining] = useState(false);
  const [showJoinForm, setShowJoinForm] = useState(false);
  const [isSwitching, setIsSwitching] = useState(false);

  const activeOrg = organizations.find(org => org.id === activeTenantId);

  useEffect(() => {
    loadOrganizations();
    fetchTenants();
  }, [token]);

  useEffect(() => {
    if (user?.email && !joinEmail) {
      setJoinEmail(user.email);
    }
  }, [user]);

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

  const handleJoinOrganization = async () => {
    if (!token || !joinOrgName.trim() || !joinEmail.trim()) return;
    setIsJoining(true);
    try {
      const data = await apiPost<Organization>('/api/organizations/join', token, {
        email: joinEmail.trim(),
        organizationName: joinOrgName.trim(),
      });
      setOrganizations([...organizations, data]);
      setJoinOrgName('');
      setJoinEmail('');
      setShowJoinForm(false);
      // Refresh tenant list
      await refreshTenants();
      alert('Successfully joined organization!');
    } catch (error: any) {
      alert(error.message || 'Failed to join organization');
    } finally {
      setIsJoining(false);
    }
  };

  const handleSwitchOrganization = async (orgId: string) => {
    if (!token || orgId === activeTenantId || !orgId) return;
    setIsSwitching(true);
    try {
      await setActiveTenant(orgId);
      await loadOrganizations();
      await fetchTenants();
      // Reload page to refresh all data with new org context
      window.location.reload();
    } catch (error: any) {
      alert(error.message || 'Failed to switch organization');
      setIsSwitching(false);
    }
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
                  Switch Organization
                </label>
                <select
                  value={activeTenantId || ''}
                  onChange={(e) => handleSwitchOrganization(e.target.value)}
                  disabled={isSwitching || organizations.length <= 1}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                >
                  {organizations.map((org) => (
                    <option key={org.id} value={org.id}>
                      {org.name} {org.id === activeTenantId ? '(Current)' : ''}
                    </option>
                  ))}
                </select>
                {organizations.length <= 1 && (
                  <p className="text-xs text-gray-500 mt-1">
                    You need at least 2 organizations to switch
                  </p>
                )}
              </div>
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
            <div className="space-y-4">
              <p className="text-gray-600">No active organization</p>
              {organizations.length > 0 && (
                <div>
                  <label className="text-sm font-medium text-gray-700 mb-1 block">
                    Select an Organization
                  </label>
                  <select
                    value=""
                    onChange={(e) => handleSwitchOrganization(e.target.value)}
                    disabled={isSwitching}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                  >
                    <option value="">Select an organization...</option>
                    {organizations.map((org) => (
                      <option key={org.id} value={org.id}>
                        {org.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>
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
              <Button
                variant="secondary"
                onClick={() => {
                  setShowJoinForm(!showJoinForm);
                  if (!showJoinForm && user?.email) {
                    setJoinEmail(user.email);
                  }
                }}
              >
                {showJoinForm ? 'Cancel Join' : 'Join Organization'}
              </Button>
            </div>

            {showJoinForm && (
              <div className="p-4 border border-gray-200 rounded-lg bg-gray-50 space-y-3">
                <div>
                  <label className="text-sm font-medium text-gray-700 mb-1 block">
                    Your Email
                  </label>
                  <Input
                    type="email"
                    placeholder="your.email@example.com"
                    value={joinEmail}
                    onChange={(e) => setJoinEmail(e.target.value)}
                  />
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-700 mb-1 block">
                    Organization Name
                  </label>
                  <Input
                    placeholder="Enter organization name to join"
                    value={joinOrgName}
                    onChange={(e) => setJoinOrgName(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && handleJoinOrganization()}
                  />
                </div>
                <Button
                  onClick={handleJoinOrganization}
                  disabled={!joinOrgName.trim() || !joinEmail.trim() || isJoining}
                >
                  {isJoining ? 'Joining...' : 'Join'}
                </Button>
              </div>
            )}

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
                        <div className="flex items-center gap-2">
                          <div>
                            <div className="font-medium text-gray-900 flex items-center gap-2">
                              {org.name}
                              {org.id === activeTenantId && (
                                <span className="px-2 py-0.5 text-xs font-medium bg-blue-100 text-blue-800 rounded">
                                  Active
                                </span>
                              )}
                            </div>
                            <div className="text-sm text-gray-500">{org.domain}</div>
                          </div>
                        </div>
                      )}
                    </div>
                    {editingOrgId !== org.id && (
                      <div className="flex gap-2">
                        {org.id !== activeTenantId && (
                          <Button
                            variant="primary"
                            onClick={() => handleSwitchOrganization(org.id)}
                            disabled={isSwitching}
                            size="sm"
                          >
                            {isSwitching ? 'Switching...' : 'Switch'}
                          </Button>
                        )}
                        <Button
                          variant="secondary"
                          onClick={() => startEditing(org)}
                          size="sm"
                        >
                          Edit
                        </Button>
                      </div>
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
