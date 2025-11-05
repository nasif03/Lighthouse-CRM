import Input from '../components/ui/Input';
import Button from '../components/ui/Button';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import { useTenantStore } from '../store/tenantStore';
import { useState } from 'react';

export default function Settings() {
  const { tenants, activeTenantId } = useTenantStore();
  const active = tenants.find(t => t.id === activeTenantId)!;
  const [name, setName] = useState(active.name);

  return (
    <Card>
      <CardHeader>Workspace Settings</CardHeader>
      <CardContent>
        <div className="max-w-md flex flex-col gap-3">
          <label className="text-sm">
            <div className="mb-1 text-gray-600">Workspace Name</div>
            <Input value={name} onChange={(e) => setName(e.target.value)} />
          </label>
          <Button variant="secondary">Save</Button>
        </div>
      </CardContent>
    </Card>
  );
}


