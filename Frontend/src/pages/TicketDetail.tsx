import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Textarea from '../components/ui/Textarea';
import Select from '../components/ui/Select';
import Modal from '../components/ui/Modal';
import { clsx } from 'clsx';

type Ticket = {
  id: string;
  ticketNumber: string;
  subject: string;
  description: string;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  company: string;
  priority: 'Low' | 'Medium' | 'High' | 'Critical';
  status: 'Open' | 'In Progress' | 'Resolved' | 'Closed';
  category: string;
  assignedTo: string;
  assignedToId: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
  slaResponseTime?: string;
  slaResolutionTime?: string;
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

type Activity = {
  id: string;
  type: 'status_change' | 'assignment' | 'priority_change' | 'comment' | 'created';
  user: string;
  description: string;
  timestamp: string;
};

// Mock data - replace with API calls
const mockTicket: Ticket = {
  id: '1',
  ticketNumber: 'T-2024-001',
  subject: 'Login Issue - Cannot access account',
  description: 'I have been unable to log into my account for the past 3 days. I keep getting an error message saying "Invalid credentials" but I am certain I am using the correct password. I have tried resetting my password multiple times but the reset emails are not arriving. This is preventing me from accessing important data.',
  customerName: 'John Doe',
  customerEmail: 'john@example.com',
  customerPhone: '+1 (555) 123-4567',
  company: 'Acme Corp',
  priority: 'High',
  status: 'In Progress',
  category: 'Technical',
  assignedTo: 'Sarah Johnson',
  assignedToId: 'agent-1',
  createdAt: '2024-01-15T10:30:00Z',
  updatedAt: '2024-01-15T14:20:00Z',
  slaResponseTime: '2 hours',
  slaResolutionTime: '24 hours',
};

const mockComments: Comment[] = [
  {
    id: '1',
    author: 'John Doe',
    authorId: 'customer-1',
    authorType: 'customer',
    content: 'I have also noticed that the password reset link in the email is not working when I click it.',
    isInternal: false,
    createdAt: '2024-01-15T11:00:00Z',
  },
  {
    id: '2',
    author: 'Sarah Johnson',
    authorId: 'agent-1',
    authorType: 'agent',
    content: 'Thank you for the additional information. I have escalated this to our technical team. They are investigating the email delivery issue.',
    isInternal: false,
    createdAt: '2024-01-15T12:15:00Z',
  },
  {
    id: '3',
    author: 'Sarah Johnson',
    authorId: 'agent-1',
    authorType: 'agent',
    content: 'Internal note: Customer account shows multiple failed login attempts. Possible account lockout. Checking with security team.',
    isInternal: true,
    createdAt: '2024-01-15T12:20:00Z',
  },
];

const mockActivities: Activity[] = [
  { id: '1', type: 'created', user: 'John Doe', description: 'Ticket created', timestamp: '2024-01-15T10:30:00Z' },
  { id: '2', type: 'assignment', user: 'System', description: 'Assigned to Sarah Johnson', timestamp: '2024-01-15T10:35:00Z' },
  { id: '3', type: 'status_change', user: 'Sarah Johnson', description: 'Status changed from Open to In Progress', timestamp: '2024-01-15T12:00:00Z' },
  { id: '4', type: 'comment', user: 'John Doe', description: 'Customer added a comment', timestamp: '2024-01-15T11:00:00Z' },
  { id: '5', type: 'comment', user: 'Sarah Johnson', description: 'Agent replied to customer', timestamp: '2024-01-15T12:15:00Z' },
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

export default function TicketDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [ticket] = useState<Ticket>(mockTicket);
  const [comments, setComments] = useState<Comment[]>(mockComments);
  const [newComment, setNewComment] = useState('');
  const [isInternalNote, setIsInternalNote] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [selectedAssignee, setSelectedAssignee] = useState(ticket.assignedToId);
  const [selectedStatus, setSelectedStatus] = useState(ticket.status);

  const handleAddComment = () => {
    if (!newComment.trim()) return;

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

  const handleAssign = () => {
    // TODO: API call to assign ticket
    alert(`Ticket assigned to ${selectedAssignee}`);
    setShowAssignModal(false);
  };

  const handleStatusChange = () => {
    // TODO: API call to update status
    alert(`Ticket status changed to ${selectedStatus}`);
    setShowStatusModal(false);
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
                      {ticket.priority}
                    </span>
                    <span className={clsx('px-2 py-1 rounded text-xs font-medium', statusColors[ticket.status])}>
                      {ticket.status}
                    </span>
                  </div>
                  <h1 className="text-2xl font-semibold">{ticket.subject}</h1>
                  <div className="flex items-center gap-4 mt-2 text-sm text-gray-500">
                    <span>Category: {ticket.category}</span>
                    <span>•</span>
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

          {/* Attachments Section */}
          <Card>
            <CardHeader>
              <h2 className="text-lg font-semibold">Attachments</h2>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                {/* Mock attachments - replace with actual data */}
                <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-md">
                  <svg className="h-8 w-8 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-medium text-gray-700">screenshot.png</div>
                    <div className="text-xs text-gray-500">2.3 MB • Uploaded by John Doe</div>
                  </div>
                  <Button variant="ghost" size="sm">Download</Button>
                </div>
                <div className="text-sm text-gray-500 text-center py-2">
                  No other attachments
                </div>
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
                {comments.map((comment) => (
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
                ))}
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
                <div className="font-medium">{ticket.customerName}</div>
              </div>
              <div>
                <div className="text-gray-500">Email</div>
                <div>{ticket.customerEmail}</div>
              </div>
              <div>
                <div className="text-gray-500">Phone</div>
                <div>{ticket.customerPhone}</div>
              </div>
              {ticket.company && (
                <div>
                  <div className="text-gray-500">Company</div>
                  <div>{ticket.company}</div>
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
                <div className="font-medium">{ticket.assignedTo || 'Unassigned'}</div>
              </div>
              <Button variant="secondary" size="sm" className="w-full mt-2" onClick={() => setShowAssignModal(true)}>
                Reassign
              </Button>
            </CardContent>
          </Card>

          {/* SLA Info */}
          {ticket.slaResponseTime && (
            <Card>
              <CardHeader>
                <h3 className="text-sm font-semibold">SLA Information</h3>
              </CardHeader>
              <CardContent className="space-y-2 text-sm">
                <div>
                  <div className="text-gray-500">Response Time</div>
                  <div>{ticket.slaResponseTime}</div>
                </div>
                <div>
                  <div className="text-gray-500">Resolution Time</div>
                  <div>{ticket.slaResolutionTime}</div>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Activity Timeline */}
          <Card>
            <CardHeader>
              <h3 className="text-sm font-semibold">Activity Timeline</h3>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {mockActivities.map((activity) => (
                  <div key={activity.id} className="flex gap-3">
                    <div className="flex-shrink-0 w-2 h-2 rounded-full bg-brand-600 mt-2" />
                    <div className="flex-1 min-w-0">
                      <div className="text-sm text-gray-700">{activity.description}</div>
                      <div className="text-xs text-gray-500 mt-1">
                        {activity.user} • {formatDateTime(activity.timestamp)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
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
              <option value="agent-1">Sarah Johnson</option>
              <option value="agent-2">Mike Chen</option>
              <option value="agent-3">Emily Davis</option>
              <option value="team-dev">Dev Team</option>
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
            <Select value={selectedStatus} onChange={(e) => setSelectedStatus(e.target.value as Ticket['status'])}>
              <option value="Open">Open</option>
              <option value="In Progress">In Progress</option>
              <option value="Resolved">Resolved</option>
              <option value="Closed">Closed</option>
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

