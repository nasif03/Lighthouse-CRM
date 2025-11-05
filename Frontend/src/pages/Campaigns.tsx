import Button from '../components/ui/Button';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import Tabs from '../components/ui/Tabs';

const list = [
  { id: 'CMP-101', name: 'Black Friday Teasers', status: 'Scheduled' },
  { id: 'CMP-102', name: 'Welcome Series', status: 'Active' },
];

export default function Campaigns() {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-2">
        <Button>New Campaign</Button>
      </div>
      <Card>
        <CardHeader>Campaigns</CardHeader>
        <CardContent>
          <Tabs
            tabs={[
              { id: 'active', label: 'Active', content: <CampaignList items={list.filter(i => i.status !== 'Completed')} /> },
              { id: 'completed', label: 'Completed', content: <CampaignList items={[]} /> },
            ]}
          />
        </CardContent>
      </Card>
    </div>
  );
}

function CampaignList({ items }: { items: { id: string; name: string; status: string }[] }) {
  return (
    <ul className="divide-y divide-gray-200">
      {items.map((i) => (
        <li key={i.id} className="py-2 flex items-center justify-between">
          <div>
            <div className="font-medium">{i.name}</div>
            <div className="text-xs text-gray-500">{i.id}</div>
          </div>
          <div className="text-sm text-gray-600">{i.status}</div>
        </li>
      ))}
      {items.length === 0 && <li className="py-6 text-sm text-gray-500">No items</li>}
    </ul>
  );
}


