import Card, { CardContent, CardHeader } from '../components/ui/Card';
import { Table, THead, TBody, TR, TH, TD } from '../components/ui/Table';

const deals = [
  { id: 'D1', name: 'Enterprise Deal', value: '$50,000', stage: 'Proposal', probability: 60 },
  { id: 'D2', name: 'SMB Contract', value: '$15,000', stage: 'Negotiation', probability: 80 },
];

export default function Deals() {
  return (
    <Card>
      <CardHeader>Deals</CardHeader>
      <CardContent>
        <Table>
          <THead>
            <tr>
              <TH>Deal Name</TH>
              <TH>Value</TH>
              <TH>Stage</TH>
              <TH>Probability</TH>
            </tr>
          </THead>
          <TBody>
            {deals.map(d => (
              <TR key={d.id}>
                <TD>{d.name}</TD>
                <TD>{d.value}</TD>
                <TD>{d.stage}</TD>
                <TD>{d.probability}%</TD>
              </TR>
            ))}
          </TBody>
        </Table>
      </CardContent>
    </Card>
  );
}

