import Card, { CardContent, CardHeader } from '../components/ui/Card';

export default function Dashboard() {
  return (
    <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
      <Card>
        <CardHeader>Active Campaigns</CardHeader>
        <CardContent>
          <div className="text-3xl font-semibold">8</div>
          <div className="text-sm text-gray-500">+2 this week</div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>New Leads</CardHeader>
        <CardContent>
          <div className="text-3xl font-semibold">142</div>
          <div className="text-sm text-gray-500">Last 7 days</div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>Email Open Rate</CardHeader>
        <CardContent>
          <div className="text-3xl font-semibold">32%</div>
          <div className="text-sm text-gray-500">Average</div>
        </CardContent>
      </Card>
    </div>
  );
}


