import Card, { CardContent, CardHeader } from '../components/ui/Card';

export default function Administration() {
  return (
    <div className="grid md:grid-cols-2 gap-4">
      <Card>
        <CardHeader>Users</CardHeader>
        <CardContent>
          <div className="text-sm text-gray-600">Manage team members and permissions</div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>Roles & Permissions</CardHeader>
        <CardContent>
          <div className="text-sm text-gray-600">Configure access levels</div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>Workspace Settings</CardHeader>
        <CardContent>
          <div className="text-sm text-gray-600">Configure workspace preferences</div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>Integrations</CardHeader>
        <CardContent>
          <div className="text-sm text-gray-600">Connect third-party services</div>
        </CardContent>
      </Card>
    </div>
  );
}

