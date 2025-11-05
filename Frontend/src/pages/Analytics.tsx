import Card, { CardContent, CardHeader } from '../components/ui/Card';

export default function Analytics() {
  return (
    <div className="grid md:grid-cols-2 gap-4">
      <Card>
        <CardHeader>Campaign Performance</CardHeader>
        <CardContent>
          <div className="h-40 rounded bg-gray-100" />
        </CardContent>
      </Card>
      <Card>
        <CardHeader>Lead Sources</CardHeader>
        <CardContent>
          <div className="h-40 rounded bg-gray-100" />
        </CardContent>
      </Card>
    </div>
  );
}


