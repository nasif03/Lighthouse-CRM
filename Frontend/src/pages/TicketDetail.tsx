import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Textarea from '../components/ui/Textarea';
import Select from '../components/ui/Select';
import Modal from '../components/ui/Modal';
import { clsx } from 'clsx';
import { useAuthStore } from '../store/authStore';
import { apiGet, apiPut, apiPost } from '../utils/api';

type Ticket = {
  id: string;
  ticketNumber: string;
  subject: string;
  description: string;
  name: string;
  email: string;
  phone?: string;
  priority: 'low' | 'medium' | 'high' | 'urgent';
  status: 'open' | 'in_progress' | 'resolved' | 'closed';
  category: string | null;
  assignedTo: string | null;
  assignedToName: string | null;
  jiraIssueKey?: string | null;
  jiraIssueUrl?: string | null;
  createdAt: string;
  updatedAt: string;
};

type Employee = {
  id: string;
  name: string;
  email: string;
  picture?: string;
};

type Comment = {
  id: string;
  author: string;
  authorId: string;
  authorType: 'agent' | 'customer';
  content: string;
  isInternal: boolean;
  createdAt: string;
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

export default function TicketDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { token } = useAuthStore();
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [comments, setComments] = useState<Comment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [newComment, setNewComment] = useState('');
  const [isInternalNote, setIsInternalNote] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [selectedAssignee, setSelectedAssignee] = useState<string>('unassigned');
  const [selectedStatus, setSelectedStatus] = useState<string>('open');
  const [selectedPriority, setSelectedPriority] = useState<string>('medium');
  const [isCreatingJiraIssue, setIsCreatingJiraIssue] = useState(false);

  useEffect(() => {
    if (token && id) {
      fetchTicket();
      fetchAssignableEmployees();
    }
  }, [token, id]);

  const fetchTicket = async () => {
    if (!token || !id) return;
    setIsLoading(true);
    setError(null);
    try {
      const data = await apiGet<Ticket>(`/api/tickets/${id}`, token);
      setTicket(data);
      setSelectedStatus(data.status);
      setSelectedPriority(data.priority);
      setSelectedAssignee(data.assignedTo || 'unassigned');
    } catch (err: any) {
      console.error('Error fetching ticket:', err);
      setError(err.message || 'Failed to fetch ticket');
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
      console.error('Error fetching assignable employees:', err);
    }
  };

  const handleAssign = async () => {
    if (!token || !id) return;
    try {
      const employeeId = selectedAssignee === 'unassigned' ? null : selectedAssignee;
      await apiPut(`/api/tickets/${id}`, token, {
        assignedTo: employeeId,
      });
      await fetchTicket();
      setShowAssignModal(false);
    } catch (err: any) {
      alert(err.message || 'Failed to assign ticket');
    }
  };

  const handleStatusChange = async () => {
    if (!token || !id) return;
    try {
      await apiPut(`/api/tickets/${id}`, token, {
        status: selectedStatus,
      });
      await fetchTicket();
      setShowStatusModal(false);
    } catch (err: any) {
      alert(err.message || 'Failed to update ticket status');
    }
  };

  const handlePriorityChange = async (priority: string) => {
    if (!token || !id) return;
    try {
      await apiPut(`/api/tickets/${id}`, token, {
        priority: priority,
      });
      await fetchTicket();
    } catch (err: any) {
      alert(err.message || 'Failed to update ticket priority');
    }
  };

  const handleCreateJiraIssue = async () => {
    if (!token || !id) return;
    setIsCreatingJiraIssue(true);
    try {
      const result = await apiPost(`/api/jira/tickets/${id}/create-issue`, token, {});
      alert(result.message || 'Jira issue created successfully!');
      await fetchTicket();
    } catch (err: any) {
      alert(err.message || 'Failed to create Jira issue');
    } finally {
      setIsCreatingJiraIssue(false);
    }
  };

  const handleAddComment = () => {
    if (!newComment.trim()) return;

    // TODO: Implement comment API endpoint
    const comment: Comment = {
      id: Date.now().toString(),
      author: 'Current User', // Replace with actual user
      authorId: 'current-user',
      authorType: 'agent',
      content: newComment,
      isInternal: isInternalNote,
      createdAt: new Date().toISOString(),
    };

    setComments([...comments, comment]);
    setNewComment('');
    setIsInternalNote(false);
  };

  const formatDateTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="text-center py-8 text-gray-500">Loading ticket...</div>
      </div>
    );
  }

  if (error || !ticket) {
    return (
      <div className="space-y-4">
        <div className="flex items-center gap-3">
          <Button variant="ghost" onClick={() => navigate('/support')}>
            ← Back to Tickets
          </Button>
        </div>
        <Card>
          <CardContent className="p-8 text-center">
            <div className="text-red-600 mb-4">
              <svg className="w-16 h-16 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Error Loading Ticket</h2>
            <p className="text-gray-600">{error || 'Ticket not found'}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Header with Back Button */}
      <div className="flex items-center gap-3">
        <Button variant="ghost" onClick={() => navigate('/support')}>
          ← Back to Tickets
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Main Content - Left 2 columns */}
        <div className="lg:col-span-2 space-y-4">
          {/* Ticket Header */}
          <Card>
            <CardHeader>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="font-mono text-sm text-gray-500">{ticket.ticketNumber}</span>
                    <span className={clsx('px-2 py-1 rounded text-xs font-medium', priorityColors[ticket.priority])}>
                      {priorityLabels[ticket.priority]}
                    </span>
                    <span className={clsx('px-2 py-1 rounded text-xs font-medium', statusColors[ticket.status])}>
                      {statusLabels[ticket.status]}
                    </span>
                  </div>
                  <h1 className="text-2xl font-semibold">{ticket.subject}</h1>
                  <div className="flex items-center gap-4 mt-2 text-sm text-gray-500">
                    {ticket.category && <span>Category: {ticket.category}</span>}
                    {ticket.category && <span>•</span>}
                    <span>Created: {formatDateTime(ticket.createdAt)}</span>
                    <span>•</span>
                    <span>Last updated: {formatDateTime(ticket.updatedAt)}</span>
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button variant="secondary" size="sm" onClick={() => setShowStatusModal(true)}>
                    Change Status
                  </Button>
                  <Button variant="secondary" size="sm" onClick={() => setShowAssignModal(true)}>
                    Assign
                  </Button>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="prose max-w-none">
                <h3 className="text-sm font-semibold text-gray-700 mb-2">Description</h3>
                <p className="text-gray-700 whitespace-pre-wrap">{ticket.description}</p>
              </div>
            </CardContent>
          </Card>

          {/* Comments Section */}
          <Card>
            <CardHeader>
              <h2 className="text-lg font-semibold">Comments & Updates</h2>
            </CardHeader>
            <CardContent>
              <div className="space-y-4 mb-4">
                {comments.length === 0 ? (
                  <div className="text-center py-4 text-gray-500 text-sm">
                    No comments yet. Be the first to add a comment.
                  </div>
                ) : (
                  comments.map((comment) => (
                    <div
                      key={comment.id}
                      className={clsx(
                        'p-4 rounded-lg border',
                        comment.isInternal
                          ? 'bg-yellow-50 border-yellow-200'
                          : comment.authorType === 'agent'
                          ? 'bg-blue-50 border-blue-200'
                          : 'bg-gray-50 border-gray-200'
                      )}
                    >
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <span className="font-medium text-sm">{comment.author}</span>
                          {comment.isInternal && (
                            <span className="px-2 py-0.5 bg-yellow-200 text-yellow-800 text-xs rounded">
                              Internal Note
                            </span>
                          )}
                          {comment.authorType === 'agent' && !comment.isInternal && (
                            <span className="px-2 py-0.5 bg-blue-200 text-blue-800 text-xs rounded">
                              Agent
                            </span>
                          )}
                        </div>
                        <span className="text-xs text-gray-500">{formatDateTime(comment.createdAt)}</span>
                      </div>
                      <p className="text-sm text-gray-700 whitespace-pre-wrap">{comment.content}</p>
                    </div>
                  ))
                )}
              </div>

              {/* Add Comment Form */}
              <div className="border-t pt-4">
                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="internalNote"
                      checked={isInternalNote}
                      onChange={(e) => setIsInternalNote(e.target.checked)}
                      className="rounded border-gray-300"
                    />
                    <label htmlFor="internalNote" className="text-sm text-gray-700">
                      Internal note (not visible to customer)
                    </label>
                  </div>
                  <Textarea
                    value={newComment}
                    onChange={(e) => setNewComment(e.target.value)}
                    placeholder={isInternalNote ? 'Add an internal note...' : 'Add a comment or reply...'}
                    rows={4}
                  />
                  <div className="flex justify-end">
                    <Button onClick={handleAddComment} disabled={!newComment.trim()}>
                      {isInternalNote ? 'Add Note' : 'Add Comment'}
                    </Button>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Sidebar - Right column */}
        <div className="space-y-4">
          {/* Customer Info */}
          <Card>
            <CardHeader>
              <h3 className="text-sm font-semibold">Customer Information</h3>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              <div>
                <div className="text-gray-500">Name</div>
                <div className="font-medium">{ticket.name}</div>
              </div>
              <div>
                <div className="text-gray-500">Email</div>
                <div>{ticket.email}</div>
              </div>
              {ticket.phone && (
                <div>
                  <div className="text-gray-500">Phone</div>
                  <div>{ticket.phone}</div>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Assignment Info */}
          <Card>
            <CardHeader>
              <h3 className="text-sm font-semibold">Assignment</h3>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              <div>
                <div className="text-gray-500">Assigned To</div>
                <div className="font-medium">{ticket.assignedToName || 'Unassigned'}</div>
              </div>
              <Button variant="secondary" size="sm" className="w-full mt-2" onClick={() => setShowAssignModal(true)}>
                Reassign
              </Button>
            </CardContent>
          </Card>

          {/* Priority */}
          <Card>
            <CardHeader>
              <h3 className="text-sm font-semibold">Priority</h3>
            </CardHeader>
            <CardContent>
              <Select
                value={ticket.priority}
                onChange={(e) => handlePriorityChange(e.target.value)}
                className="w-full"
              >
                <option value="low">Low</option>
                <option value="medium">Medium</option>
                <option value="high">High</option>
                <option value="urgent">Urgent</option>
              </Select>
            </CardContent>
          </Card>

          {/* Jira Integration */}
          <Card>
            <CardHeader>
              <h3 className="text-sm font-semibold">Jira Integration</h3>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              {ticket.jiraIssueKey ? (
                <div>
                  <div className="text-gray-500 mb-2">Jira Issue</div>
                  <a
                    href={ticket.jiraIssueUrl || '#'}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:text-blue-800 font-medium inline-flex items-center gap-1"
                  >
                    {ticket.jiraIssueKey}
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                    </svg>
                  </a>
                </div>
              ) : (
                <div>
                  <div className="text-gray-500 mb-2">No Jira issue linked</div>
                  <Button
                    variant="secondary"
                    size="sm"
                    className="w-full"
                    onClick={handleCreateJiraIssue}
                    disabled={isCreatingJiraIssue}
                  >
                    {isCreatingJiraIssue ? 'Creating...' : 'Create Jira Issue'}
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Assign Modal */}
      <Modal open={showAssignModal} onClose={() => setShowAssignModal(false)} title="Assign Ticket">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Assign To</label>
            <Select value={selectedAssignee} onChange={(e) => setSelectedAssignee(e.target.value)}>
              <option value="unassigned">Unassigned</option>
              {employees.map(emp => (
                <option key={emp.id} value={emp.id}>{emp.name}</option>
              ))}
            </Select>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setShowAssignModal(false)}>
              Cancel
            </Button>
            <Button onClick={handleAssign}>Assign</Button>
          </div>
        </div>
      </Modal>

      {/* Status Change Modal */}
      <Modal open={showStatusModal} onClose={() => setShowStatusModal(false)} title="Change Status">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
            <Select value={selectedStatus} onChange={(e) => setSelectedStatus(e.target.value)}>
              <option value="open">Open</option>
              <option value="in_progress">In Progress</option>
              <option value="resolved">Resolved</option>
              <option value="closed">Closed</option>
            </Select>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setShowStatusModal(false)}>
              Cancel
            </Button>
            <Button onClick={handleStatusChange}>Update Status</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
