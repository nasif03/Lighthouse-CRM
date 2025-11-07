import { useState, useEffect } from 'react';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { useAuthStore } from '../store/authStore';
import { useTenantStore } from '../store/tenantStore';
import { apiGet, apiPost, apiPut, apiDelete } from '../utils/api';

type Employee = {
  id: string;
  name: string;
  email: string;
  picture?: string;
  roleIds: string[];
  isAdmin: boolean;
  createdAt: string;
};

type Role = {
  id: string;
  name: string;
  permissions: string[];
  orgId: string;
  createdAt: string;
  updatedAt: string;
};

export default function Administration() {
  const { token } = useAuthStore();
  const { activeTenantId } = useTenantStore();
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [isLoadingEmployees, setIsLoadingEmployees] = useState(false);
  const [isLoadingRoles, setIsLoadingRoles] = useState(false);
  
  // Employee management
  const [isAddingEmployee, setIsAddingEmployee] = useState(false);
  const [newEmployeeName, setNewEmployeeName] = useState('');
  const [newEmployeeEmail, setNewEmployeeEmail] = useState('');
  const [selectedRoleIds, setSelectedRoleIds] = useState<string[]>([]);
  
  // Role management
  const [isAddingRole, setIsAddingRole] = useState(false);
  const [newRoleName, setNewRoleName] = useState('');
  const [newRolePermissions, setNewRolePermissions] = useState<string[]>([]);
  const [editingEmployeeId, setEditingEmployeeId] = useState<string | null>(null);
  const [editingEmployeeRoles, setEditingEmployeeRoles] = useState<string[]>([]);

  const availablePermissions = [
    'read:leads',
    'write:leads',
    'read:contacts',
    'write:contacts',
    'read:deals',
    'write:deals',
    'read:accounts',
    'write:accounts',
    'read:campaigns',
    'write:campaigns',
    'read:tickets',
    'write:tickets',
    'admin:users',
    'admin:roles',
  ];

  useEffect(() => {
    if (activeTenantId && token) {
      loadEmployees();
      loadRoles();
    }
  }, [activeTenantId, token]);

  const loadEmployees = async () => {
    if (!activeTenantId || !token) return;
    setIsLoadingEmployees(true);
    try {
      const data = await apiGet<Employee[]>(`/api/organizations/${activeTenantId}/employees`, token);
      setEmployees(data);
    } catch (error: any) {
      if (error.message?.includes('403')) {
        alert('You do not have permission to view employees. Only organization admins can manage employees.');
      } else {
        console.error('Error loading employees:', error);
      }
    } finally {
      setIsLoadingEmployees(false);
    }
  };

  const loadRoles = async () => {
    if (!activeTenantId || !token) return;
    setIsLoadingRoles(true);
    try {
      const data = await apiGet<Role[]>(`/api/organizations/${activeTenantId}/roles`, token);
      setRoles(data);
    } catch (error) {
      console.error('Error loading roles:', error);
    } finally {
      setIsLoadingRoles(false);
    }
  };

  const handleAddEmployee = async () => {
    if (!activeTenantId || !token || !newEmployeeName.trim() || !newEmployeeEmail.trim()) return;
    setIsAddingEmployee(true);
    try {
      const data = await apiPost<Employee>(
        `/api/organizations/${activeTenantId}/employees`,
        token,
        {
          name: newEmployeeName.trim(),
          email: newEmployeeEmail.trim(),
          roleIds: selectedRoleIds,
        }
      );
      setEmployees([...employees, data]);
      setNewEmployeeName('');
      setNewEmployeeEmail('');
      setSelectedRoleIds([]);
    } catch (error: any) {
      alert(error.message || 'Failed to add employee');
    } finally {
      setIsAddingEmployee(false);
    }
  };

  const handleUpdateEmployeeRoles = async (employeeId: string) => {
    if (!activeTenantId || !token) return;
    try {
      const data = await apiPut<Employee>(
        `/api/organizations/${activeTenantId}/employees/${employeeId}`,
        token,
        {
          roleIds: editingEmployeeRoles,
        }
      );
      setEmployees(employees.map(emp => emp.id === employeeId ? data : emp));
      setEditingEmployeeId(null);
      setEditingEmployeeRoles([]);
    } catch (error: any) {
      alert(error.message || 'Failed to update employee roles');
    }
  };

  const handleAddRole = async () => {
    if (!activeTenantId || !token || !newRoleName.trim()) return;
    setIsAddingRole(true);
    try {
      const data = await apiPost<Role>(
        `/api/organizations/${activeTenantId}/roles`,
        token,
        {
          name: newRoleName.trim(),
          permissions: newRolePermissions,
        }
      );
      setRoles([...roles, data]);
      setNewRoleName('');
      setNewRolePermissions([]);
    } catch (error: any) {
      alert(error.message || 'Failed to create role');
    } finally {
      setIsAddingRole(false);
    }
  };

  const handleDeleteRole = async (roleId: string) => {
    if (!activeTenantId || !token) return;
    if (!confirm('Are you sure you want to delete this role?')) return;
    try {
      await apiDelete(`/api/organizations/${activeTenantId}/roles/${roleId}`, token);
      setRoles(roles.filter(role => role.id !== roleId));
      // Remove role from employees
      setEmployees(employees.map(emp => ({
        ...emp,
        roleIds: emp.roleIds.filter(rid => rid !== roleId),
      })));
    } catch (error: any) {
      alert(error.message || 'Failed to delete role');
    }
  };

  const togglePermission = (permission: string) => {
    if (newRolePermissions.includes(permission)) {
      setNewRolePermissions(newRolePermissions.filter(p => p !== permission));
    } else {
      setNewRolePermissions([...newRolePermissions, permission]);
    }
  };

  const startEditingEmployee = (employee: Employee) => {
    setEditingEmployeeId(employee.id);
    setEditingEmployeeRoles([...employee.roleIds]);
  };

  if (!activeTenantId) {
    return (
      <Card>
        <CardContent>
          <p className="text-gray-600">Please select an organization to manage employees and roles.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="grid md:grid-cols-2 gap-6">
      <Card>
        <CardHeader>Employees</CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="border-b border-gray-200 pb-4">
              <h3 className="text-sm font-medium text-gray-900 mb-3">Add Employee</h3>
              <div className="space-y-2">
                <Input
                  placeholder="Employee Name"
                  value={newEmployeeName}
                  onChange={(e) => setNewEmployeeName(e.target.value)}
                />
                <Input
                  type="email"
                  placeholder="Employee Email"
                  value={newEmployeeEmail}
                  onChange={(e) => setNewEmployeeEmail(e.target.value)}
                />
                <div>
                  <label className="text-sm text-gray-700 mb-1 block">Roles</label>
                  <div className="space-y-1 max-h-32 overflow-y-auto">
                    {roles.map(role => (
                      <label key={role.id} className="flex items-center gap-2">
                        <input
                          type="checkbox"
                          checked={selectedRoleIds.includes(role.id)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedRoleIds([...selectedRoleIds, role.id]);
                            } else {
                              setSelectedRoleIds(selectedRoleIds.filter(id => id !== role.id));
                            }
                          }}
                        />
                        <span className="text-sm text-gray-700">{role.name}</span>
                      </label>
                    ))}
                  </div>
                </div>
                <Button
                  onClick={handleAddEmployee}
                  disabled={!newEmployeeName.trim() || !newEmployeeEmail.trim() || isAddingEmployee}
                  className="w-full"
                >
                  {isAddingEmployee ? 'Adding...' : 'Add Employee'}
                </Button>
              </div>
            </div>

            <div>
              <h3 className="text-sm font-medium text-gray-900 mb-3">Employee List</h3>
              {isLoadingEmployees ? (
                <p className="text-gray-600">Loading employees...</p>
              ) : employees.length === 0 ? (
                <p className="text-gray-600">No employees found.</p>
              ) : (
                <div className="space-y-2">
                  {employees.map(employee => (
                    <div
                      key={employee.id}
                      className="flex items-center justify-between p-3 border border-gray-200 rounded-lg"
                    >
                      <div className="flex-1">
                        <div className="font-medium text-gray-900">{employee.name}</div>
                        <div className="text-sm text-gray-500">{employee.email}</div>
                        <div className="text-xs text-gray-400 mt-1">
                          {employee.isAdmin && <span className="text-brand-600">Admin</span>}
                          {employee.roleIds.length > 0 && (
                            <span className="ml-2">
                              Roles: {employee.roleIds.map(rid => {
                                const role = roles.find(r => r.id === rid);
                                return role?.name;
                              }).filter(Boolean).join(', ')}
                            </span>
                          )}
                        </div>
                      </div>
                      {editingEmployeeId === employee.id ? (
                        <div className="flex gap-2">
                          <div className="space-y-1 max-h-32 overflow-y-auto border border-gray-200 p-2 rounded">
                            {roles.map(role => (
                              <label key={role.id} className="flex items-center gap-2">
                                <input
                                  type="checkbox"
                                  checked={editingEmployeeRoles.includes(role.id)}
                                  onChange={(e) => {
                                    if (e.target.checked) {
                                      setEditingEmployeeRoles([...editingEmployeeRoles, role.id]);
                                    } else {
                                      setEditingEmployeeRoles(editingEmployeeRoles.filter(id => id !== role.id));
                                    }
                                  }}
                                />
                                <span className="text-sm text-gray-700">{role.name}</span>
                              </label>
                            ))}
                          </div>
                          <Button
                            onClick={() => handleUpdateEmployeeRoles(employee.id)}
                            size="sm"
                          >
                            Save
                          </Button>
                          <Button
                            variant="secondary"
                            onClick={() => {
                              setEditingEmployeeId(null);
                              setEditingEmployeeRoles([]);
                            }}
                            size="sm"
                          >
                            Cancel
                          </Button>
                        </div>
                      ) : (
                        <Button
                          variant="secondary"
                          onClick={() => startEditingEmployee(employee)}
                          size="sm"
                        >
                          Edit Roles
                        </Button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>Roles & Permissions</CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="border-b border-gray-200 pb-4">
              <h3 className="text-sm font-medium text-gray-900 mb-3">Create Role</h3>
              <div className="space-y-2">
                <Input
                  placeholder="Role Name"
                  value={newRoleName}
                  onChange={(e) => setNewRoleName(e.target.value)}
                />
                <div>
                  <label className="text-sm text-gray-700 mb-1 block">Permissions</label>
                  <div className="space-y-1 max-h-48 overflow-y-auto border border-gray-200 p-2 rounded">
                    {availablePermissions.map(permission => (
                      <label key={permission} className="flex items-center gap-2">
                        <input
                          type="checkbox"
                          checked={newRolePermissions.includes(permission)}
                          onChange={() => togglePermission(permission)}
                        />
                        <span className="text-sm text-gray-700">{permission}</span>
                      </label>
                    ))}
                  </div>
                </div>
                <Button
                  onClick={handleAddRole}
                  disabled={!newRoleName.trim() || isAddingRole}
                  className="w-full"
                >
                  {isAddingRole ? 'Creating...' : 'Create Role'}
                </Button>
              </div>
            </div>

            <div>
              <h3 className="text-sm font-medium text-gray-900 mb-3">Roles List</h3>
              {isLoadingRoles ? (
                <p className="text-gray-600">Loading roles...</p>
              ) : roles.length === 0 ? (
                <p className="text-gray-600">No roles found. Create one above.</p>
              ) : (
                <div className="space-y-2">
                  {roles.map(role => (
                    <div
                      key={role.id}
                      className="p-3 border border-gray-200 rounded-lg"
                    >
                      <div className="flex items-center justify-between mb-2">
                        <div className="font-medium text-gray-900">{role.name}</div>
                        <Button
                          variant="secondary"
                          onClick={() => handleDeleteRole(role.id)}
                          size="sm"
                        >
                          Delete
                        </Button>
                      </div>
                      <div className="text-xs text-gray-500">
                        Permissions: {role.permissions.join(', ') || 'None'}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
