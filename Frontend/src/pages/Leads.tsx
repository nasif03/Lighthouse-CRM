import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import { Table, THead, TBody, TR, TH, TD } from '../components/ui/Table';
import Modal from '../components/ui/Modal';
import { useState, useEffect } from 'react';
import { useAuthStore } from '../store/authStore';

const API_BASE_URL = 'http://localhost:3000';

const STATUS_OPTIONS = [
  { value: 'new', label: 'New' },
  { value: 'contacted', label: 'Contacted' },
  { value: 'qualified', label: 'Qualified' },
  { value: 'converted', label: 'Converted' },
  { value: 'lost', label: 'Lost' },
];

type Lead = {
  id: string;
  name: string;
  email: string;
  source: string;
  status: string;
  ownerId: string;
  orgId: string;
  createdAt: string;
  updatedAt: string;
};

export default function Leads() {
  const { token } = useAuthStore();
  const [query, setQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    source: '',
    status: 'new',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [leads, setLeads] = useState<Lead[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch leads on component mount and when token changes
  useEffect(() => {
    fetchLeads();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const fetchLeads = async () => {
    if (!token) {
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const response = await fetch(`${API_BASE_URL}/api/leads`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error('Failed to fetch leads');
      }

      const data = await response.json();
      setLeads(data);
    } catch (err: any) {
      console.error('Error fetching leads:', err);
      setError(err.message || 'Failed to load leads');
    } finally {
      setIsLoading(false);
    }
  };

  const filteredLeads = leads.filter(l => 
    l.name.toLowerCase().includes(query.toLowerCase()) ||
    l.email.toLowerCase().includes(query.toLowerCase())
  );

  const handleInputChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);
    
    if (!token) {
      setError('You must be logged in to create a lead');
      setIsSubmitting(false);
      return;
    }

    try {
      console.log('Creating lead with data:', formData);
      
      const response = await fetch(`${API_BASE_URL}/api/leads`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: formData.name,
          email: formData.email,
          source: formData.source,
          status: formData.status,
        }),
      });

      const responseData = await response.json().catch(() => null);

      if (!response.ok) {
        const errorMessage = responseData?.detail || responseData?.message || `HTTP ${response.status}: ${response.statusText}`;
        console.error('Lead creation failed:', errorMessage);
        throw new Error(errorMessage);
      }

      console.log('Lead created successfully:', responseData);

      // Reset form and close modal
      setFormData({ name: '', email: '', source: '', status: 'new' });
      setIsModalOpen(false);
      
      // Refresh leads list
      await fetchLeads();
    } catch (error: any) {
      console.error('Error creating lead:', error);
      const errorMessage = error.message || 'Failed to create lead. Please check your connection and try again.';
      setError(errorMessage);
      // Keep modal open so user can see the error and try again
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    if (!isSubmitting) {
      setFormData({ name: '', email: '', source: '', status: 'new' });
      setIsModalOpen(false);
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-2">
        <Input placeholder="Search leads" value={query} onChange={(e) => setQuery(e.target.value)} />
        <Button onClick={() => {
          setError(null);
          setIsModalOpen(true);
        }}>New Lead</Button>
      </div>
      <Card>
        <CardHeader>Leads</CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-gray-600">Loading leads...</div>
          ) : error ? (
            <div className="text-center py-8 text-red-600">{error}</div>
          ) : filteredLeads.length === 0 ? (
            <div className="text-center py-8 text-gray-600">
              {query ? 'No leads found matching your search' : 'No leads yet. Create your first lead!'}
            </div>
          ) : (
            <Table>
              <THead>
                <tr>
                  <TH>Name</TH>
                  <TH>Email</TH>
                  <TH>Source</TH>
                  <TH>Status</TH>
                </tr>
              </THead>
              <TBody>
                {filteredLeads.map(l => (
                  <TR key={l.id}>
                    <TD>{l.name}</TD>
                    <TD>{l.email}</TD>
                    <TD>{l.source}</TD>
                    <TD>
                      <span className="capitalize">{l.status}</span>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Modal open={isModalOpen} onClose={handleClose} title="Create New Lead">
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {error && (
            <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">
              {error}
            </div>
          )}
          <div className="flex flex-col gap-2">
            <label htmlFor="name" className="text-sm font-medium text-gray-700">
              Name
            </label>
            <Input
              id="name"
              type="text"
              placeholder="Enter lead name"
              value={formData.name}
              onChange={(e) => handleInputChange('name', e.target.value)}
              required
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="email" className="text-sm font-medium text-gray-700">
              Email
            </label>
            <Input
              id="email"
              type="email"
              placeholder="Enter email address"
              value={formData.email}
              onChange={(e) => handleInputChange('email', e.target.value)}
              required
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="source" className="text-sm font-medium text-gray-700">
              Source
            </label>
            <Input
              id="source"
              type="text"
              placeholder="Enter lead source"
              value={formData.source}
              onChange={(e) => handleInputChange('source', e.target.value)}
              required
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="status" className="text-sm font-medium text-gray-700">
              Status
            </label>
            <select
              id="status"
              className="h-9 px-3 rounded-md border border-gray-200 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500 shadow-sm"
              value={formData.status}
              onChange={(e) => handleInputChange('status', e.target.value)}
              required
            >
              {STATUS_OPTIONS.map(option => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="secondary"
              onClick={handleClose}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Creating...' : 'Create Lead'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}


