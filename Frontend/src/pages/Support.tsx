import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Select from '../components/ui/Select';
import { Table, THead, TBody, TR, TH, TD } from '../components/ui/Table';
import { clsx } from 'clsx';
import { useAuthStore } from '../store/authStore';
import { useTenantStore } from '../store/tenantStore';
import { apiGet, apiPut } from '../utils/api';

type Ticket = {
  id: string;
  ticketNumber: string;
  subject: string;
  name: string;
  email: string;
  phone?: string;
  priority: 'low' | 'medium' | 'high' | 'urgent';
  status: 'open' | 'in_progress' | 'resolved' | 'closed';
  assignedTo: string | null;
  assignedToName: string | null;
  category: string | null;
  createdAt: string;
  updatedAt: string;
};

type Employee = {
  id: string;
  name: string;
  email: string;
  picture?: string;
};

const priorityColors = {
  low: 'bg-gray-100 text-gray-700',
  medium: 'bg-blue-100 text-blue-700',
  high: 'bg-orange-100 text-orange-700',
  urgent: 'bg-red-100 text-red-700',
};

const statusColors = {
  open: 'bg-yellow-100 text-yellow-700',
  in_progress: 'bg-blue-100 text-blue-700',
  resolved: 'bg-green-100 text-green-700',
  closed: 'bg-gray-100 text-gray-700',
};

const priorityLabels = {
  low: 'Low',
  medium: 'Medium',
  high: 'High',
  urgent: 'Urgent',
};

const statusLabels = {
  open: 'Open',
  in_progress: 'In Progress',
  resolved: 'Resolved',
  closed: 'Closed',
};

export default function Support() {
  const navigate = useNavigate();
  const { token } = useAuthStore();
  const { activeTenantId } = useTenantStore();
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [priorityFilter, setPriorityFilter] = useState<string>('all');
  const [selectedTickets, setSelectedTickets] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (token ) {
      fetchTickets();
      fetchAssignableEmployees();
    }
  }, [token]);

  const fetchTickets = async () => {
    if (!token) return;
    setIsLoading(true);
    setError(null);
    try {
      const data = await apiGet<Ticket[]>('/api/tickets', token);
      setTickets(data);
    } catch (err: any) {
      if (err.message === 'Request cancelled') {
        return;
      }
      console.error('Error fetching tickets:', err);
      setError(err.message || 'Failed to fetch tickets');
    } finally {
      setIsLoading(false);
    }
  };

  const fetchAssignableEmployees = async () => {
    if (!token) return;
    try {
      const data = await apiGet<Employee[]>('/api/tickets/assignable-employees', token);
      setEmployees(data);
    } catch (err: any) {
      if (err.message === 'Request cancelled') {
        return;
      }
      console.error('Error fetching assignable employees:', err);
    }
  };

  const handleAssignTicket = async (ticketId: string, employeeId: string | null) => {
    if (!token) return;
    try {
      await apiPut(`/api/tickets/${ticketId}`, token, {
        assignedTo: employeeId,
      });
      await fetchTickets();
    } catch (err: any) {
      alert(err.message || 'Failed to assign ticket');
    }
  };

  const handleStatusChange = async (ticketId: string, status: string) => {
    if (!token) return;
    try {
      await apiPut(`/api/tickets/${ticketId}`, token, {
        status: status,
      });
      await fetchTickets();
    } catch (err: any) {
      alert(err.message || 'Failed to update ticket status');
    }
  };

  // Filter and sort tickets
  const filteredTickets = tickets
    .filter(ticket => {
      const matchesSearch = 
        ticket.ticketNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
        ticket.subject.toLowerCase().includes(searchQuery.toLowerCase()) ||
        ticket.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        ticket.email.toLowerCase().includes(searchQuery.toLowerCase());
      
      const matchesStatus = statusFilter === 'all' || ticket.status === statusFilter;
      const matchesPriority = priorityFilter === 'all' || ticket.priority === priorityFilter;
      
      return matchesSearch && matchesStatus && matchesPriority;
    })
    .sort((a, b) => {
      const dateA = new Date(a.updatedAt).getTime();
      const dateB = new Date(b.updatedAt).getTime();
      return dateB - dateA; // Newest first
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
    total: tickets.length,
    open: tickets.filter(t => t.status === 'open').length,
    inProgress: tickets.filter(t => t.status === 'in_progress').length,
    resolved: tickets.filter(t => t.status === 'resolved' || t.status === 'closed').length,
  };

  if (error && error.includes('permission')) {
    return (
      <div className="space-y-4">
        <Card>
          <CardContent className="p-8 text-center">
            <div className="text-red-600 mb-4">
              <svg className="w-16 h-16 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Access Denied</h2>
            <p className="text-gray-600 mb-4">{error}</p>
            <p className="text-sm text-gray-500">
              Please contact your administrator to assign you a role with ticket permissions (read:tickets or write:tickets).
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

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
          {error && !error.includes('permission') && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700 text-sm">
              {error}
            </div>
          )}

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
                <option value="open">Open</option>
                <option value="in_progress">In Progress</option>
                <option value="resolved">Resolved</option>
                <option value="closed">Closed</option>
              </Select>
              <Select
                value={priorityFilter}
                onChange={(e) => setPriorityFilter(e.target.value)}
                className="w-full md:w-48"
              >
                <option value="all">All Priorities</option>
                <option value="urgent">Urgent</option>
                <option value="high">High</option>
                <option value="medium">Medium</option>
                <option value="low">Low</option>
              </Select>
            </div>

            {/* Bulk Actions */}
            {selectedTickets.size > 0 && (
              <div className="flex items-center gap-2 p-2 bg-blue-50 rounded-md">
                <span className="text-sm text-blue-700">
                  {selectedTickets.size} ticket{selectedTickets.size > 1 ? 's' : ''} selected
                </span>
                <div className="flex gap-2 ml-auto">
                  <Button variant="secondary" size="sm" onClick={() => {
                    const employeeId = prompt('Enter employee ID to assign:');
                    if (employeeId) {
                      selectedTickets.forEach(ticketId => {
                        handleAssignTicket(ticketId, employeeId);
                      });
                    }
                  }}>
                    Assign
                  </Button>
                  <Button variant="secondary" size="sm" onClick={() => {
                    selectedTickets.forEach(ticketId => {
                      handleStatusChange(ticketId, 'closed');
                    });
                    setSelectedTickets(new Set());
                  }}>
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
          {isLoading ? (
            <div className="text-center py-8 text-gray-500">Loading tickets...</div>
          ) : (
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
                        {tickets.length === 0 ? 'No tickets found' : 'No tickets match your filters'}
                      </TD>
                    </tr>
                  ) : (
                    filteredTickets.map((ticket) => (
                      <TR
                        key={ticket.id}
                        className="cursor-pointer hover:bg-gray-50"
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
                          {ticket.category && (
                            <div className="text-xs text-gray-500">{ticket.category}</div>
                          )}
                        </TD>
                        <TD>
                          <div>{ticket.name}</div>
                          <div className="text-xs text-gray-500">{ticket.email}</div>
                        </TD>
                        <TD>
                          <span className={clsx('px-2 py-1 rounded text-xs font-medium', priorityColors[ticket.priority])}>
                            {priorityLabels[ticket.priority]}
                          </span>
                        </TD>
                        <TD>
                          <span className={clsx('px-2 py-1 rounded text-xs font-medium', statusColors[ticket.status])}>
                            {statusLabels[ticket.status]}
                          </span>
                        </TD>
                        <TD>
                          <div className="text-sm">
                            {ticket.assignedToName || 'Unassigned'}
                          </div>
                        </TD>
                        <TD>
                          <span className="text-sm text-gray-600">{formatDate(ticket.createdAt)}</span>
                        </TD>
                        <TD>
                          <span className="text-sm text-gray-600">{formatDate(ticket.updatedAt)}</span>
                        </TD>
                        <TD onClick={(e) => e.stopPropagation()}>
                          <div className="flex gap-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => navigate(`/support/${ticket.id}`)}
                            >
                              View
                            </Button>
                            <Select
                              value={ticket.assignedTo || 'unassigned'}
                              onChange={(e) => {
                                e.stopPropagation();
                                const employeeId = e.target.value === 'unassigned' ? null : e.target.value;
                                handleAssignTicket(ticket.id, employeeId);
                              }}
                              onClick={(e) => e.stopPropagation()}
                              className="text-xs"
                            >
                              <option value="unassigned">Unassigned</option>
                              {employees.map(emp => (
                                <option key={emp.id} value={emp.id}>{emp.name}</option>
                              ))}
                            </Select>
                          </div>
                        </TD>
                      </TR>
                    ))
                  )}
                </TBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
