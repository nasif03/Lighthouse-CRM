import Card, { CardContent, CardHeader } from '../components/ui/Card';
import { Table, THead, TBody, TR, TH, TD } from '../components/ui/Table';

const contacts = [
  { id: 'C1', name: 'Jane Cooper', company: 'Acme Inc', email: 'jane@acme.com' },
  { id: 'C2', name: 'Jacob Jones', company: 'Globex Co', email: 'jacob@globex.com' },
];

export default function Contacts() {
  return (
    <Card>
      <CardHeader>Contacts</CardHeader>
      <CardContent>
        <Table>
          <THead>
            <tr>
              <TH>Name</TH>
              <TH>Company</TH>
              <TH>Email</TH>
            </tr>
          </THead>
          <TBody>
            {contacts.map(c => (
              <TR key={c.id}>
                <TD>{c.name}</TD>
                <TD>{c.company}</TD>
                <TD>{c.email}</TD>
              </TR>
            ))}
          </TBody>
        </Table>
      </CardContent>
    </Card>
  );
}


