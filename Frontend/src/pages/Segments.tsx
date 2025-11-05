import Button from '../components/ui/Button';
import Card, { CardContent, CardHeader } from '../components/ui/Card';

const segments = [
  { id: 'SEG-1', name: 'Engaged last 30 days', size: 1240 },
  { id: 'SEG-2', name: 'High value customers', size: 312 },
];

export default function Segments() {
  return (
    <Card>
      <CardHeader>Segments</CardHeader>
      <CardContent>
        <div className="flex items-center gap-2 mb-3">
          <Button>New Segment</Button>
        </div>
        <ul className="divide-y divide-gray-200">
          {segments.map(s => (
            <li key={s.id} className="py-2 flex items-center justify-between">
              <div>
                <div className="font-medium">{s.name}</div>
                <div className="text-xs text-gray-500">{s.id}</div>
              </div>
              <div className="text-sm text-gray-600">{s.size} contacts</div>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}


