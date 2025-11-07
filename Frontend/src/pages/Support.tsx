import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Select from '../components/ui/Select';
import { Table, THead, TBody, TR, TH, TD } from '../components/ui/Table';
import { clsx } from 'clsx';

type Ticket = {
  id: string;
  ticketNumber: string;
  subject: string;
  customerName: string;
  customerEmail: string;
  priority: 'Low' | 'Medium' | 'High' | 'Critical';
  status: 'Open' | 'In Progress' | 'Resolved' | 'Closed';
  assignedTo: string;
  category: string;
  createdAt: string;
  updatedAt: string;
};

// Mock data - replace with API call
const mockTickets: Ticket[] = [
  {
    id: '1',
    ticketNumber: 'T-2024-001',
    subject: 'Login Issue - Cannot access account',
    customerName: 'John Doe',
    customerEmail: 'john@example.com',
    priority: 'High',
    status: 'Open',
    assignedTo: 'Unassigned',
    category: 'Technical',
    createdAt: '2024-01-15T10:30:00Z',
    updatedAt: '2024-01-15T10:30:00Z',
  },
  {
    id: '2',
    ticketNumber: 'T-2024-002',
    subject: 'Billing Question - Invoice discrepancy',
    customerName: 'Jane Smith',
    customerEmail: 'jane@example.com',
    priority: 'Medium',
    status: 'In Progress',
    assignedTo: 'Sarah Johnson',
    category: 'Billing',
    createdAt: '2024-01-14T14:20:00Z',
    updatedAt: '2024-01-15T09:15:00Z',
  },
  {
    id: '3',
    ticketNumber: 'T-2024-003',
    subject: 'Feature Request - Export functionality',
    customerName: 'Mike Wilson',
    customerEmail: 'mike@example.com',
    priority: 'Low',
    status: 'Resolved',
    assignedTo: 'Dev Team',
    category: 'Feature Request',
    createdAt: '2024-01-10T08:00:00Z',
    updatedAt: '2024-01-14T16:45:00Z',
  },
  {
    id: '4',
    ticketNumber: 'T-2024-004',
    subject: 'Critical Bug - Data loss issue',
    customerName: 'Emily Brown',
    customerEmail: 'emily@example.com',
    priority: 'Critical',
    status: 'Open',
    assignedTo: 'Tech Lead',
    category: 'Bug Report',
    createdAt: '2024-01-15T11:00:00Z',
    updatedAt: '2024-01-15T11:00:00Z',
  },
];

const priorityColors = {
  Low: 'bg-gray-100 text-gray-700',
  Medium: 'bg-blue-100 text-blue-700',
  High: 'bg-orange-100 text-orange-700',
  Critical: 'bg-red-100 text-red-700',
};

const statusColors = {
  Open: 'bg-yellow-100 text-yellow-700',
  'In Progress': 'bg-blue-100 text-blue-700',
  Resolved: 'bg-green-100 text-green-700',
  Closed: 'bg-gray-100 text-gray-700',
};

export default function Support() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [priorityFilter, setPriorityFilter] = useState<string>('all');
  const [selectedTickets, setSelectedTickets] = useState<Set<string>>(new Set());
  const [sortBy, setSortBy] = useState<'date' | 'priority'>('date');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');

  // Filter and sort tickets
  const filteredTickets = mockTickets
    .filter(ticket => {
      const matchesSearch = 
        ticket.ticketNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
        ticket.subject.toLowerCase().includes(searchQuery.toLowerCase()) ||
        ticket.customerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        ticket.customerEmail.toLowerCase().includes(searchQuery.toLowerCase());
      
      const matchesStatus = statusFilter === 'all' || ticket.status === statusFilter;
      const matchesPriority = priorityFilter === 'all' || ticket.priority === priorityFilter;
      
      return matchesSearch && matchesStatus && matchesPriority;
    })
    .sort((a, b) => {
      if (sortBy === 'date') {
        const dateA = new Date(a.updatedAt).getTime();
        const dateB = new Date(b.updatedAt).getTime();
        return sortOrder === 'asc' ? dateA - dateB : dateB - dateA;
      } else {
        const priorityOrder = { Critical: 4, High: 3, Medium: 2, Low: 1 };
        const priorityA = priorityOrder[a.priority];
        const priorityB = priorityOrder[b.priority];
        return sortOrder === 'asc' ? priorityA - priorityB : priorityB - priorityA;
      }
    });

  const handleSelectTicket = (ticketId: string) => {
    const newSelected = new Set(selectedTickets);
    if (newSelected.has(ticketId)) {
      newSelected.delete(ticketId);
    } else {
      newSelected.add(ticketId);
    }
    setSelectedTickets(newSelected);
  };

  const handleSelectAll = () => {
    if (selectedTickets.size === filteredTickets.length) {
      setSelectedTickets(new Set());
    } else {
      setSelectedTickets(new Set(filteredTickets.map(t => t.id)));
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  };

  const stats = {
    total: mockTickets.length,
    open: mockTickets.filter(t => t.status === 'Open').length,
    inProgress: mockTickets.filter(t => t.status === 'In Progress').length,
    resolved: mockTickets.filter(t => t.status === 'Resolved' || t.status === 'Closed').length,
  };

  return (
    <div className="space-y-4">
      {/* Stats Dashboard */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="text-sm text-gray-500">Total Tickets</div>
            <div className="text-2xl font-semibold mt-1">{stats.total}</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-sm text-gray-500">Open</div>
            <div className="text-2xl font-semibold text-yellow-600 mt-1">{stats.open}</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-sm text-gray-500">In Progress</div>
            <div className="text-2xl font-semibold text-blue-600 mt-1">{stats.inProgress}</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-sm text-gray-500">Resolved</div>
            <div className="text-2xl font-semibold text-green-600 mt-1">{stats.resolved}</div>
          </CardContent>
        </Card>
      </div>

      {/* Main Ticket Management Card */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-semibold">Support Tickets</h2>
              <p className="text-sm text-gray-500 mt-1">Manage and track customer support requests</p>
            </div>
            <Button onClick={() => navigate('/support/create')}>
              New Ticket
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {/* Filters and Search */}
          <div className="mb-4 space-y-3">
            <div className="flex flex-col md:flex-row gap-3">
              <div className="flex-1">
                <Input
                  placeholder="Search by ticket ID, subject, customer name, or email..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full"
                />
              </div>
              <Select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="w-full md:w-48"
              >
                <option value="all">All Statuses</option>
                <option value="Open">Open</option>
                <option value="In Progress">In Progress</option>
                <option value="Resolved">Resolved</option>
                <option value="Closed">Closed</option>
              </Select>
              <Select
                value={priorityFilter}
                onChange={(e) => setPriorityFilter(e.target.value)}
                className="w-full md:w-48"
              >
                <option value="all">All Priorities</option>
                <option value="Critical">Critical</option>
                <option value="High">High</option>
                <option value="Medium">Medium</option>
                <option value="Low">Low</option>
              </Select>
              <Select
                value={`${sortBy}-${sortOrder}`}
                onChange={(e) => {
                  const [by, order] = e.target.value.split('-');
                  setSortBy(by as 'date' | 'priority');
                  setSortOrder(order as 'asc' | 'desc');
                }}
                className="w-full md:w-48"
              >
                <option value="date-desc">Sort: Newest First</option>
                <option value="date-asc">Sort: Oldest First</option>
                <option value="priority-desc">Sort: Priority High</option>
                <option value="priority-asc">Sort: Priority Low</option>
              </Select>
            </div>

            {/* Bulk Actions */}
            {selectedTickets.size > 0 && (
              <div className="flex items-center gap-2 p-2 bg-blue-50 rounded-md">
                <span className="text-sm text-blue-700">
                  {selectedTickets.size} ticket{selectedTickets.size > 1 ? 's' : ''} selected
                </span>
                <div className="flex gap-2 ml-auto">
                  <Button variant="secondary" size="sm" onClick={() => alert('Bulk assign - TODO')}>
                    Assign
                  </Button>
                  <Button variant="secondary" size="sm" onClick={() => alert('Bulk close - TODO')}>
                    Close
                  </Button>
                  <Button variant="secondary" size="sm" onClick={() => setSelectedTickets(new Set())}>
                    Clear
                  </Button>
                </div>
              </div>
            )}
          </div>

          {/* Tickets Table */}
          <div className="overflow-x-auto">
            <Table>
              <THead>
                <tr>
                  <TH>
                    <input
                      type="checkbox"
                      checked={selectedTickets.size === filteredTickets.length && filteredTickets.length > 0}
                      onChange={handleSelectAll}
                      className="rounded border-gray-300"
                    />
                  </TH>
                  <TH>Ticket ID</TH>
                  <TH>Subject</TH>
                  <TH>Customer</TH>
                  <TH>Priority</TH>
                  <TH>Status</TH>
                  <TH>Assigned To</TH>
                  <TH>Created</TH>
                  <TH>Last Updated</TH>
                  <TH>Actions</TH>
                </tr>
              </THead>
              <TBody>
                {filteredTickets.length === 0 ? (
                  <tr>
                    <TD colSpan={10} className="text-center py-8 text-gray-500">
                      No tickets found
                    </TD>
                  </tr>
                ) : (
                  filteredTickets.map((ticket) => (
                    <TR
                      key={ticket.id}
                      className="cursor-pointer"
                      onClick={() => navigate(`/support/${ticket.id}`)}
                    >
                      <TD onClick={(e) => e.stopPropagation()}>
                        <input
                          type="checkbox"
                          checked={selectedTickets.has(ticket.id)}
                          onChange={() => handleSelectTicket(ticket.id)}
                          className="rounded border-gray-300"
                        />
                      </TD>
                      <TD>
                        <span className="font-mono text-xs text-gray-600">{ticket.ticketNumber}</span>
                      </TD>
                      <TD>
                        <div className="font-medium">{ticket.subject}</div>
                        <div className="text-xs text-gray-500">{ticket.category}</div>
                      </TD>
                      <TD>
                        <div>{ticket.customerName}</div>
                        <div className="text-xs text-gray-500">{ticket.customerEmail}</div>
                      </TD>
                      <TD>
                        <span className={clsx('px-2 py-1 rounded text-xs font-medium', priorityColors[ticket.priority])}>
                          {ticket.priority}
                        </span>
                      </TD>
                      <TD>
                        <span className={clsx('px-2 py-1 rounded text-xs font-medium', statusColors[ticket.status])}>
                          {ticket.status}
                        </span>
                      </TD>
                      <TD>
                        <span className="text-sm">{ticket.assignedTo}</span>
                      </TD>
                      <TD>
                        <span className="text-sm text-gray-600">{formatDate(ticket.createdAt)}</span>
                      </TD>
                      <TD>
                        <span className="text-sm text-gray-600">{formatDate(ticket.updatedAt)}</span>
                      </TD>
                      <TD onClick={(e) => e.stopPropagation()}>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => navigate(`/support/${ticket.id}`)}
                        >
                          View
                        </Button>
                      </TD>
                    </TR>
                  ))
                )}
              </TBody>
            </Table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
