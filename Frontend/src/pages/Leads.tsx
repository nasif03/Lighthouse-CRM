import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import { Table, THead, TBody, TR, TH, TD } from '../components/ui/Table';
import { useState } from 'react';

const mockLeads = Array.from({ length: 8 }).map((_, i) => ({
  id: `L${i + 1}`,
  name: `Lead ${i + 1}`,
  email: `lead${i + 1}@example.com`,
  status: i % 2 === 0 ? 'New' : 'Contacted',
}));

export default function Leads() {
  const [query, setQuery] = useState('');
  const leads = mockLeads.filter(l => l.name.toLowerCase().includes(query.toLowerCase()));

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-2">
        <Input placeholder="Search leads" value={query} onChange={(e) => setQuery(e.target.value)} />
        <Button>New Lead</Button>
      </div>
      <Card>
        <CardHeader>Leads</CardHeader>
        <CardContent>
          <Table>
            <THead>
              <tr>
                <TH>Name</TH>
                <TH>Email</TH>
                <TH>Status</TH>
              </tr>
            </THead>
            <TBody>
              {leads.map(l => (
                <TR key={l.id}>
                  <TD>{l.name}</TD>
                  <TD>{l.email}</TD>
                  <TD>{l.status}</TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}


