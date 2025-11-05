import Card, { CardContent, CardHeader } from '../components/ui/Card';
import Tabs from '../components/ui/Tabs';

const tickets = [
  { id: 'T1', subject: 'Login Issue', priority: 'High', status: 'Open', assigned: 'Support Team' },
  { id: 'T2', subject: 'Feature Request', priority: 'Low', status: 'In Progress', assigned: 'Dev Team' },
];

export default function Support() {
  return (
    <Card>
      <CardHeader>Customer Support</CardHeader>
      <CardContent>
        <Tabs
          tabs={[
            { 
              id: 'open', 
              label: 'Open Tickets', 
              content: (
                <ul className="divide-y divide-gray-200">
                  {tickets.filter(t => t.status === 'Open').map(t => (
                    <li key={t.id} className="py-2">
                      <div className="font-medium">{t.subject}</div>
                      <div className="text-xs text-gray-500">{t.priority} Priority</div>
                    </li>
                  ))}
                </ul>
              )
            },
            { 
              id: 'in-progress', 
              label: 'In Progress', 
              content: (
                <ul className="divide-y divide-gray-200">
                  {tickets.filter(t => t.status === 'In Progress').map(t => (
                    <li key={t.id} className="py-2">
                      <div className="font-medium">{t.subject}</div>
                      <div className="text-xs text-gray-500">Assigned to {t.assigned}</div>
                    </li>
                  ))}
                </ul>
              )
            },
          ]}
        />
      </CardContent>
    </Card>
  );
}

