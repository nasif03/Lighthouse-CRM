import Button from '../components/ui/Button';
import Card, { CardContent, CardHeader } from '../components/ui/Card';

const templates = [
  { id: 'TMP-1', name: 'Welcome Email' },
  { id: 'TMP-2', name: 'Promo - 20% Off' },
];

export default function Templates() {
  return (
    <Card>
      <CardHeader>Email Templates</CardHeader>
      <CardContent>
        <div className="flex items-center gap-2 mb-3">
          <Button>New Template</Button>
        </div>
        <ul className="divide-y divide-gray-200">
          {templates.map(t => (
            <li key={t.id} className="py-2 flex items-center justify-between">
              <div className="font-medium">{t.name}</div>
              <div className="text-xs text-gray-500">{t.id}</div>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}


