import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Modal from '../components/ui/Modal';
import { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import { useAuthStore } from '../store/authStore';
import { apiGet, apiPost, apiPatch, apiDelete, clearCache } from '../utils/api';
import { useDebounce } from '../hooks/useDebounce';

const STATUS_OPTIONS = [
  { value: 'new', label: 'New', color: 'bg-blue-50 border-blue-200' },
  { value: 'contacted', label: 'Contacted', color: 'bg-yellow-50 border-yellow-200' },
  { value: 'qualified', label: 'Qualified', color: 'bg-green-50 border-green-200' },
  { value: 'converted', label: 'Converted', color: 'bg-purple-50 border-purple-200' },
  { value: 'lost', label: 'Lost', color: 'bg-red-50 border-red-200' },
];

// Kanban columns (excluding converted - it's a separate column)
const KANBAN_STATUSES = STATUS_OPTIONS.filter(s => s.value !== 'converted' && s.value !== 'lost');

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

// Helper function to parse CSV file
const parseCSV = (text: string): Array<Record<string, string>> => {
  const lines = text.split('\n').filter(line => line.trim());
  if (lines.length === 0) return [];
  
  // Parse header
  const headers = lines[0].split(',').map(h => h.trim().replace(/^"|"$/g, ''));
  
  // Parse rows
  const rows: Array<Record<string, string>> = [];
  for (let i = 1; i < lines.length; i++) {
    const values = lines[i].split(',').map(v => v.trim().replace(/^"|"$/g, ''));
    if (values.length === 0 || values.every(v => !v)) continue;
    
    const row: Record<string, string> = {};
    headers.forEach((header, index) => {
      row[header.toLowerCase()] = values[index] || '';
    });
    rows.push(row);
  }
  
  return rows;
};

// Helper function to map CSV row to lead data
const mapCSVRowToLead = (row: Record<string, string>) => {
  // Support various column name variations
  const name = row['name'] || row['full name'] || row['fullname'] || row['lead name'] || '';
  const email = row['email'] || row['email address'] || row['e-mail'] || '';
  const source = row['source'] || row['lead source'] || row['origin'] || 'Import';
  const status = row['status'] || 'new';
  const phone = row['phone'] || row['phone number'] || row['mobile'] || row['tel'] || '';
  
  return {
    name: name.trim(),
    email: email.trim(),
    source: source.trim(),
    status: status.trim().toLowerCase() || 'new',
    phone: phone.trim() || undefined,
  };
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
  const [deletingLeadId, setDeletingLeadId] = useState<string | null>(null);
  const [isImporting, setIsImporting] = useState(false);
  const [importProgress, setImportProgress] = useState({ current: 0, total: 0 });
  const fileInputRef = useRef<HTMLInputElement>(null);
  const isProcessingImportRef = useRef(false);

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
      // Keep all leads including converted ones (they'll be shown in Convert column)
      setLeads(data);
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
    // Filter leads by status (exclude converted - they go in Convert column)
    return filteredLeads.filter(lead => lead.status === status && lead.status !== 'converted');
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

  const handleImportClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileImport = useCallback(async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file || !token) {
      // Reset file input if no file
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      return;
    }

    // Prevent duplicate processing
    if (isProcessingImportRef.current) {
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      return;
    }

    isProcessingImportRef.current = true;

    // Reset file input after a short delay to prevent double-trigger
    setTimeout(() => {
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }, 100);

    const fileName = file.name.toLowerCase();
    const isCSV = fileName.endsWith('.csv');
    const isXLSX = fileName.endsWith('.xlsx') || fileName.endsWith('.xls');

    if (!isCSV && !isXLSX) {
      setError('Please select a CSV or XLSX file');
      isProcessingImportRef.current = false;
      return;
    }

    setIsImporting(true);
    setError(null);
    setImportProgress({ current: 0, total: 0 });

    try {
      let rows: Array<Record<string, string>> = [];

      if (isCSV) {
        const text = await file.text();
        rows = parseCSV(text);
      } else {
        // XLSX files require a library - show message for now
        setError('XLSX import requires additional setup. Please convert to CSV or contact support.');
        setIsImporting(false);
        isProcessingImportRef.current = false;
        return;
      }

      if (rows.length === 0) {
        setError('No data found in file. Please check the file format.');
        setIsImporting(false);
        isProcessingImportRef.current = false;
        return;
      }

      // Deduplicate rows based on email (case-insensitive)
      const seenEmails = new Set<string>();
      const uniqueRows = rows.filter(row => {
        const email = (row['email'] || row['email address'] || row['e-mail'] || '').toLowerCase().trim();
        if (!email || seenEmails.has(email)) {
          return false;
        }
        seenEmails.add(email);
        return true;
      });

      if (uniqueRows.length < rows.length) {
        const duplicates = rows.length - uniqueRows.length;
        console.log(`Removed ${duplicates} duplicate row(s) based on email`);
      }

      setImportProgress({ current: 0, total: uniqueRows.length });

      // Process leads in batches to avoid overwhelming the API
      const batchSize = 5;
      let successCount = 0;
      let errorCount = 0;
      const errors: string[] = [];

      for (let i = 0; i < uniqueRows.length; i += batchSize) {
        const batch = uniqueRows.slice(i, i + batchSize);
        
        // Process sequentially within batch to avoid request cancellation
        for (let j = 0; j < batch.length; j++) {
          const row = batch[j];
          const rowIndex = i + j;
          
          try {
            const leadData = mapCSVRowToLead(row);
            
            // Validate required fields
            if (!leadData.name || !leadData.email) {
              throw new Error(`Row ${rowIndex + 2}: Missing name or email`);
            }

            // Validate email format
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(leadData.email)) {
              throw new Error(`Row ${rowIndex + 2}: Invalid email format`);
            }

            // Use unique cache key for each request to prevent cancellation
            // Include row index and timestamp to ensure uniqueness
            const uniqueKey = `POST:/api/leads:import:${rowIndex}:${performance.now()}`;
            await apiPost('/api/leads', token, leadData, {
              cacheKey: uniqueKey,
              skipCache: true
            });
            successCount++;
          } catch (err: any) {
            errorCount++;
            if (err.message !== 'Request cancelled') {
              errors.push(err.message || `Row ${rowIndex + 2}: Failed to import`);
            }
          }
          
          setImportProgress({ current: rowIndex + 1, total: rows.length });
        }
      }

      clearCache('/api/leads');
      await fetchLeads();

      if (errorCount > 0) {
        setError(
          `Import completed: ${successCount} leads imported, ${errorCount} failed. ` +
          (errors.length > 0 ? `Errors: ${errors.slice(0, 3).join('; ')}${errors.length > 3 ? '...' : ''}` : '')
        );
      } else {
        alert(`Successfully imported ${successCount} lead(s)!`);
      }
    } catch (error: any) {
      console.error('Error importing file:', error);
      setError(error.message || 'Failed to import file. Please check the file format.');
    } finally {
      setIsImporting(false);
      setImportProgress({ current: 0, total: 0 });
      isProcessingImportRef.current = false;
    }
  }, [token, fetchLeads]);

  const handleDragStart = (e: React.DragEvent, lead: Lead) => {
    setDraggedLead(lead);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  };

  const handleDelete = useCallback(async (leadId: string, e: React.MouseEvent) => {
    e.stopPropagation(); // Prevent drag from triggering
    
    if (!confirm('Are you sure you want to delete this lead?')) {
      return;
    }

    if (!token) {
      setError('You must be logged in to delete a lead');
      return;
    }

    setDeletingLeadId(leadId);
    setError(null);

    try {
      await apiDelete(`/api/leads/${leadId}`, token);
      
      // Remove the deleted lead from the list
      setLeads(prevLeads =>
        prevLeads.filter(lead => lead.id !== leadId)
      );

      clearCache('/api/leads');
    } catch (error: any) {
      console.error('Error deleting lead:', error);
      setError(error.message || 'Failed to delete lead');
    } finally {
      setDeletingLeadId(null);
    }
  }, [token]);

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

      // Update the lead status to converted (it will appear in Convert column)
      setLeads(prevLeads =>
        prevLeads.map(lead =>
          lead.id === leadId ? { ...lead, status: 'converted' } : lead
        )
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

  const handleDrop = useCallback(async (e: React.DragEvent, newStatus: string) => {
    e.preventDefault();
    
    if (!draggedLead || !token) return;
    
    // If dropped in Convert column, trigger conversion
    if (newStatus === 'convert') {
      await handleConvert(draggedLead.id);
      setDraggedLead(null);
      return;
    }
    
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
  }, [draggedLead, token, fetchLeads, handleConvert]);

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
          <Button 
            variant="secondary"
            onClick={handleImportClick}
            disabled={isImporting}
          >
            {isImporting ? `Importing... (${importProgress.current}/${importProgress.total})` : 'Import CSV/XLSX'}
          </Button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv,.xlsx,.xls"
            onChange={handleFileImport}
            className="hidden"
          />
        </div>
      </div>

      {error && (
        <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">
          {error}
        </div>
      )}

      <div className="flex gap-3 pb-4">
        {KANBAN_STATUSES.map(statusOption => {
          const statusLeads = getLeadsByStatus(statusOption.value);
          return (
            <div
              key={statusOption.value}
              className={`flex-1 min-w-0 rounded-lg border-2 p-3 ${statusOption.color}`}
              onDragOver={handleDragOver}
              onDrop={(e) => handleDrop(e, statusOption.value)}
            >
              <div className="font-semibold mb-3 text-base text-gray-900 flex items-center justify-between">
                <span>{statusOption.label}</span>
                <span className="text-sm font-normal text-gray-600 bg-white px-2 py-0.5 rounded">
                  {statusLeads.length}
                </span>
              </div>
              <div className="flex flex-col gap-2 min-h-[150px]">
                {statusLeads.length === 0 ? (
                  <div className="text-center text-gray-400 text-xs py-4">
                    No leads
                  </div>
                ) : (
                  statusLeads.map(lead => (
                    <div
                      key={lead.id}
                      draggable
                      onDragStart={(e) => handleDragStart(e, lead)}
                      className="bg-white rounded border border-gray-200 p-2 cursor-move hover:shadow-sm transition-shadow relative group"
                    >
                      <button
                        onClick={(e) => handleDelete(lead.id, e)}
                        disabled={deletingLeadId === lead.id}
                        className="absolute top-1 right-1 w-5 h-5 flex items-center justify-center text-gray-400 hover:text-red-600 hover:bg-red-50 rounded opacity-0 group-hover:opacity-100 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed z-10"
                        title="Delete lead"
                      >
                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full bg-brand-500 text-white flex items-center justify-center text-xs font-semibold flex-shrink-0">
                          {getInitials(lead.name)}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="font-medium text-sm text-gray-900 truncate">
                            {lead.name}
                          </div>
                          <div className="text-xs text-gray-500 truncate">
                            {lead.email}
                          </div>
                        </div>
                        <div className="flex-shrink-0 text-xs text-gray-400">
                          {formatRelativeTime(lead.createdAt)}
                        </div>
                      </div>
                      <div className="flex items-center gap-1.5 mt-1.5 flex-wrap">
                        <span className="text-xs px-1.5 py-0.5 bg-gray-100 text-gray-700 rounded">
                          {lead.source}
                        </span>
                        {lead.tags && lead.tags.length > 0 && (
                          <>
                            {lead.tags.slice(0, 1).map((tag, idx) => (
                              <span key={idx} className="text-xs px-1.5 py-0.5 bg-blue-100 text-blue-700 rounded">
                                {tag}
                              </span>
                            ))}
                            {lead.tags.length > 1 && (
                              <span className="text-xs text-gray-500">+{lead.tags.length - 1}</span>
                            )}
                          </>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          );
        })}
        {/* Convert Column */}
        <div
          className="flex-1 min-w-0 rounded-lg border-2 p-3 bg-purple-50 border-purple-200"
          onDragOver={handleDragOver}
          onDrop={(e) => handleDrop(e, 'convert')}
        >
          <div className="font-semibold mb-3 text-base text-gray-900 flex items-center justify-between">
            <span>Convert</span>
            <span className="text-sm font-normal text-gray-600 bg-white px-2 py-0.5 rounded">
              {filteredLeads.filter(l => l.status === 'converted').length}
            </span>
          </div>
          <div className="flex flex-col gap-2 min-h-[150px]">
            {filteredLeads.filter(l => l.status === 'converted').length === 0 ? (
              <div className="text-center text-gray-400 text-xs py-4">
                Drop leads here to convert
              </div>
            ) : (
              filteredLeads.filter(l => l.status === 'converted').map(lead => (
                <div
                  key={lead.id}
                  className="bg-white rounded border border-gray-200 p-2 relative group"
                >
                  <button
                    onClick={(e) => handleDelete(lead.id, e)}
                    disabled={deletingLeadId === lead.id}
                    className="absolute top-1 right-1 w-5 h-5 flex items-center justify-center text-gray-400 hover:text-red-600 hover:bg-red-50 rounded opacity-0 group-hover:opacity-100 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed z-10"
                    title="Delete lead"
                  >
                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                  <div className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-full bg-purple-500 text-white flex items-center justify-center text-xs font-semibold flex-shrink-0">
                      {getInitials(lead.name)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-sm text-gray-900 truncate">
                        {lead.name}
                      </div>
                      <div className="text-xs text-gray-500 truncate">
                        {lead.email}
                      </div>
                    </div>
                    <div className="flex-shrink-0 text-xs text-purple-600 font-medium">
                      ✓ Converted
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
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
