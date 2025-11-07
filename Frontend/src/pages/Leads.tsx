import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Modal from '../components/ui/Modal';
import { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import { useAuthStore } from '../store/authStore';
import { apiGet, apiPost, apiPatch, clearCache } from '../utils/api';
import { useDebounce } from '../hooks/useDebounce';

const STATUS_OPTIONS = [
  { value: 'new', label: 'New', color: 'bg-blue-50 border-blue-200' },
  { value: 'contacted', label: 'Contacted', color: 'bg-yellow-50 border-yellow-200' },
  { value: 'qualified', label: 'Qualified', color: 'bg-green-50 border-green-200' },
  { value: 'converted', label: 'Converted', color: 'bg-purple-50 border-purple-200' },
  { value: 'lost', label: 'Lost', color: 'bg-red-50 border-red-200' },
];

// Kanban columns (excluding converted since it has a convert button)
const KANBAN_STATUSES = STATUS_OPTIONS.filter(s => s.value !== 'converted');

type Lead = {
  id: string;
  name: string;
  email: string;
  source: string;
  status: string;
  phone?: string;
  firstName?: string;
  lastName?: string;
  tags: string[];
  ownerId: string;
  orgId: string;
  createdAt: string;
  updatedAt: string;
};

// Helper function to get initials from name
const getInitials = (name: string): string => {
  const parts = name.trim().split(' ');
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
  return name.substring(0, 2).toUpperCase();
};

// Helper function to format relative time
const formatRelativeTime = (dateString: string): string => {
  const date = new Date(dateString);
  const now = new Date();
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);
  
  if (diffInSeconds < 60) return 'Just now';
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}m ago`;
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}h ago`;
  if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)}d ago`;
  
  return date.toLocaleDateString();
};

export default function Leads() {
  const { token } = useAuthStore();
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebounce(query, 300);
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
  const fetchingRef = useRef(false);
  const [draggedLead, setDraggedLead] = useState<Lead | null>(null);
  const [convertingLeadId, setConvertingLeadId] = useState<string | null>(null);

  const fetchLeads = useCallback(async () => {
    if (!token) {
      setIsLoading(false);
      return;
    }

    if (fetchingRef.current) {
      return;
    }

    fetchingRef.current = true;

    try {
      setIsLoading(true);
      setError(null);
      const data = await apiGet<Lead[]>('/api/leads', token);
      // Filter out converted leads - they appear in contacts page instead
      setLeads(data.filter(lead => lead.status !== 'converted'));
    } catch (err: any) {
      if (err.message === 'Request cancelled') {
        return;
      }
      console.error('Error fetching leads:', err);
      setError(err.message || 'Failed to load leads');
    } finally {
      setIsLoading(false);
      fetchingRef.current = false;
    }
  }, [token]);

  useEffect(() => {
    fetchLeads();
  }, [fetchLeads]);

  const filteredLeads = useMemo(() => {
    if (!debouncedQuery) return leads;
    const lowerQuery = debouncedQuery.toLowerCase();
    return leads.filter(l => 
      l.name.toLowerCase().includes(lowerQuery) ||
      l.email.toLowerCase().includes(lowerQuery) ||
      l.source.toLowerCase().includes(lowerQuery) ||
      (l.phone && l.phone.toLowerCase().includes(lowerQuery))
    );
  }, [leads, debouncedQuery]);

  const getLeadsByStatus = (status: string): Lead[] => {
    // Filter leads by status (converted leads are already filtered out)
    return filteredLeads.filter(lead => lead.status === status);
  };

  const handleInputChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);
    
    if (!token) {
      setError('You must be logged in to create a lead');
      setIsSubmitting(false);
      return;
    }

    try {
      await apiPost('/api/leads', token, {
        name: formData.name,
        email: formData.email,
        source: formData.source,
        status: formData.status,
      });

      clearCache('/api/leads');
      setFormData({ name: '', email: '', source: '', status: 'new' });
      setIsModalOpen(false);
      await fetchLeads();
    } catch (error: any) {
      console.error('Error creating lead:', error);
      const errorMessage = error.message || 'Failed to create lead. Please check your connection and try again.';
      setError(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  }, [token, formData, fetchLeads]);

  const handleClose = () => {
    if (!isSubmitting) {
      setFormData({ name: '', email: '', source: '', status: 'new' });
      setIsModalOpen(false);
    }
  };

  const handleDragStart = (e: React.DragEvent, lead: Lead) => {
    setDraggedLead(lead);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  };

  const handleDrop = useCallback(async (e: React.DragEvent, newStatus: string) => {
    e.preventDefault();
    
    if (!draggedLead || !token) return;
    
    if (draggedLead.status === newStatus) {
      setDraggedLead(null);
      return;
    }

    try {
      const updatedLead = await apiPatch<Lead>(
        `/api/leads/${draggedLead.id}/status`,
        token,
        { status: newStatus }
      );

      setLeads(prevLeads =>
        prevLeads.map(lead =>
          lead.id === draggedLead.id ? updatedLead : lead
        )
      );

      clearCache('/api/leads');
    } catch (error: any) {
      console.error('Error updating lead status:', error);
      setError(error.message || 'Failed to update lead status');
      await fetchLeads();
    } finally {
      setDraggedLead(null);
    }
  }, [draggedLead, token, fetchLeads]);

  const handleConvert = useCallback(async (leadId: string) => {
    if (!token) {
      setError('You must be logged in to convert a lead');
      return;
    }

    setConvertingLeadId(leadId);
    setError(null);

    try {
      await apiPost(
        `/api/leads/${leadId}/convert`,
        token
      );

      // Remove the converted lead from the leads list (it will appear in contacts)
      setLeads(prevLeads =>
        prevLeads.filter(lead => lead.id !== leadId)
      );

      clearCache('/api/leads');
      
      // Show success message
      alert(`Lead converted successfully! Created Account, Contact, and Deal.`);
    } catch (error: any) {
      console.error('Error converting lead:', error);
      setError(error.message || 'Failed to convert lead');
    } finally {
      setConvertingLeadId(null);
    }
  }, [token]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading leads...</div>
      </div>
    );
  }

  if (error && !filteredLeads.length) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-red-600">{error}</div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Leads Pipeline</h1>
        <div className="flex items-center gap-2">
          <Input 
            placeholder="Search leads..." 
            value={query} 
            onChange={(e) => setQuery(e.target.value)}
            className="w-64"
          />
          <Button onClick={() => {
            setError(null);
            setIsModalOpen(true);
          }}>New Lead</Button>
        </div>
      </div>

      {error && (
        <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">
          {error}
        </div>
      )}

      <div className="flex gap-4 pb-4">
        {KANBAN_STATUSES.map(statusOption => {
          const statusLeads = getLeadsByStatus(statusOption.value);
          return (
            <div
              key={statusOption.value}
              className={`flex-1 min-w-0 rounded-lg border-2 p-4 ${statusOption.color}`}
              onDragOver={handleDragOver}
              onDrop={(e) => handleDrop(e, statusOption.value)}
            >
              <div className="font-semibold mb-4 text-lg text-gray-900 flex items-center justify-between">
                <span>{statusOption.label}</span>
                <span className="text-base font-normal text-gray-600 bg-white px-2.5 py-1 rounded">
                  {statusLeads.length}
                </span>
              </div>
              <div className="flex flex-col gap-3 min-h-[200px]">
                {statusLeads.length === 0 ? (
                  <div className="text-center text-gray-400 text-xs py-6">
                    No leads
                  </div>
                ) : (
                  statusLeads.map(lead => (
                    <div
                      key={lead.id}
                      draggable
                      onDragStart={(e) => handleDragStart(e, lead)}
                      className="bg-white rounded-lg border border-gray-200 p-3 cursor-move hover:shadow-md transition-shadow"
                    >
                      <div className="flex items-start gap-2.5 mb-2.5">
                        <div className="w-9 h-9 rounded-full bg-brand-500 text-white flex items-center justify-center text-sm font-semibold flex-shrink-0">
                          {getInitials(lead.name)}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="font-medium text-base text-gray-900 truncate">
                            {lead.name}
                          </div>
                          <div className="text-sm text-gray-500 truncate">
                            {lead.email}
                          </div>
                        </div>
                      </div>

                      {lead.phone && (
                        <div className="text-sm text-gray-600 mb-2 flex items-center gap-1.5">
                          <span>📞</span>
                          <span className="truncate">{lead.phone}</span>
                        </div>
                      )}

                      <div className="flex items-center gap-2 mb-2.5 flex-wrap">
                        <span className="text-sm px-2.5 py-1 bg-gray-100 text-gray-700 rounded">
                          {lead.source}
                        </span>
                        {lead.tags && lead.tags.length > 0 && (
                          <div className="flex gap-1.5">
                            {lead.tags.slice(0, 2).map((tag, idx) => (
                              <span key={idx} className="text-sm px-2 py-0.5 bg-blue-100 text-blue-700 rounded">
                                {tag}
                              </span>
                            ))}
                            {lead.tags.length > 2 && (
                              <span className="text-sm text-gray-500">+{lead.tags.length - 2}</span>
                            )}
                          </div>
                        )}
                      </div>

                      <div className="text-sm text-gray-400 mb-2.5">
                        {formatRelativeTime(lead.createdAt)}
                      </div>

                      {lead.status !== 'converted' && (
                        <button
                          onClick={() => handleConvert(lead.id)}
                          disabled={convertingLeadId === lead.id}
                          className="w-full mt-2 px-3 py-2 text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 rounded disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                          {convertingLeadId === lead.id ? 'Converting...' : 'Convert'}
                        </button>
                      )}

                      {lead.status === 'converted' && (
                        <div className="w-full mt-2 px-3 py-2 text-sm font-medium text-center text-purple-700 bg-purple-100 rounded">
                          ✓ Converted
                        </div>
                      )}
                    </div>
                  ))
                )}
              </div>
            </div>
          );
        })}
      </div>

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
