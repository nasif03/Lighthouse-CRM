import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Modal from '../components/ui/Modal';
import { useState, useEffect, useCallback, useMemo } from 'react';
import { useAuthStore } from '../store/authStore';
import { apiGet, apiPost, apiPut, apiDelete, clearCache } from '../utils/api';
import { useDebounce } from '../hooks/useDebounce';

type Deal = {
  id: string;
  name: string;
  accountId?: string;
  contactId?: string;
  amount?: number;
  currency?: string;
  stageId?: string;
  stageName?: string;
  probability?: number;
  closeDate?: string;
  status: string;
  tags: string[];
  createdAt: string;
  updatedAt: string;
};

type Account = {
  id: string;
  name: string;
};

type Contact = {
  id: string;
  firstName: string;
  lastName?: string;
};

// Default sales stages with colors
const DEFAULT_STAGES = [
  { id: 'prospecting', name: 'Prospecting', order: 0, color: 'bg-blue-50 border-blue-200' },
  { id: 'qualification', name: 'Qualification', order: 1, color: 'bg-yellow-50 border-yellow-200' },
  { id: 'proposal', name: 'Proposal', order: 2, color: 'bg-green-50 border-green-200' },
  { id: 'negotiation', name: 'Negotiation', order: 3, color: 'bg-orange-50 border-orange-200' },
  { id: 'closed-won', name: 'Closed Won', order: 4, color: 'bg-purple-50 border-purple-200' },
  { id: 'closed-lost', name: 'Closed Lost', order: 5, color: 'bg-red-50 border-red-200' },
];

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

export default function Deals() {
  const { token } = useAuthStore();
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebounce(query, 300);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draggedDeal, setDraggedDeal] = useState<Deal | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    accountId: '',
    contactId: '',
    amount: '',
    currency: 'USD',
    stageId: 'prospecting',
    stageName: 'Prospecting',
    probability: '',
    closeDate: '',
    status: 'open',
    tags: [] as string[],
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [deals, setDeals] = useState<Deal[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [stages, setStages] = useState(DEFAULT_STAGES);
  const [updatingStageId, setUpdatingStageId] = useState<string | null>(null);

  const fetchDeals = useCallback(async () => {
    if (!token) {
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const data = await apiGet<Deal[]>('/api/deals', token);
      setDeals(data);
    } catch (err: any) {
      console.error('Error fetching deals:', err);
      setError(err.message || 'Failed to load deals');
    } finally {
      setIsLoading(false);
    }
  }, [token]);

  const fetchAccounts = useCallback(async () => {
    if (!token) return;

    try {
      const data = await apiGet<Account[]>('/api/accounts', token);
      setAccounts(data);
    } catch (err) {
      console.error('Error fetching accounts:', err);
    }
  }, [token]);

  const fetchContacts = useCallback(async () => {
    if (!token) return;

    try {
      const data = await apiGet<Contact[]>('/api/contacts', token);
      setContacts(data);
    } catch (err) {
      console.error('Error fetching contacts:', err);
    }
  }, [token]);

  useEffect(() => {
    if (token) {
      fetchDeals();
      fetchAccounts();
      fetchContacts();
    }
  }, [token, fetchDeals, fetchAccounts, fetchContacts]);

  const getAccountName = (accountId?: string) => {
    if (!accountId) return null;
    const account = accounts.find(a => a.id === accountId);
    return account?.name;
  };

  const filteredDeals = useMemo(() => {
    if (!debouncedQuery) return deals;
    const lowerQuery = debouncedQuery.toLowerCase();
    return deals.filter(d => {
      const accountName = d.accountId ? getAccountName(d.accountId) : null;
      return (
        d.name.toLowerCase().includes(lowerQuery) ||
        (accountName && accountName.toLowerCase().includes(lowerQuery)) ||
        (d.amount && d.amount.toString().includes(lowerQuery))
      );
    });
  }, [deals, debouncedQuery, accounts]);

  const getDealsByStage = (stageId: string): Deal[] => {
    return filteredDeals.filter(d => (d.stageId || 'prospecting') === stageId);
  };

  const handleDragStart = (e: React.DragEvent, deal: Deal) => {
    setDraggedDeal(deal);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  };

  const handleDrop = useCallback(async (e: React.DragEvent, targetStageId: string) => {
    e.preventDefault();
    if (!draggedDeal || !token) return;

    const targetStage = stages.find(s => s.id === targetStageId);
    if (!targetStage) return;

    if (draggedDeal.stageId === targetStageId) {
      setDraggedDeal(null);
      return;
    }

    setUpdatingStageId(draggedDeal.id);

    try {
      await apiPut(
        `/api/deals/${draggedDeal.id}`,
        token,
        {
          stageId: targetStageId,
          stageName: targetStage.name,
        }
      );

      setDeals(prevDeals =>
        prevDeals.map(deal =>
          deal.id === draggedDeal.id
            ? { ...deal, stageId: targetStageId, stageName: targetStage.name }
            : deal
        )
      );

      clearCache('/api/deals');
    } catch (err: any) {
      console.error('Error updating deal stage:', err);
      setError(err.message || 'Failed to update deal stage');
      await fetchDeals();
    } finally {
      setDraggedDeal(null);
      setUpdatingStageId(null);
    }
  }, [draggedDeal, token, stages, fetchDeals]);

  const handleEdit = (deal: Deal) => {
    setFormData({
      name: deal.name,
      accountId: deal.accountId || '',
      contactId: deal.contactId || '',
      amount: deal.amount?.toString() || '',
      currency: deal.currency || 'USD',
      stageId: deal.stageId || 'prospecting',
      stageName: deal.stageName || 'Prospecting',
      probability: deal.probability?.toString() || '',
      closeDate: deal.closeDate || '',
      status: deal.status,
      tags: deal.tags || [],
    });
    setIsEditMode(true);
    setEditingId(deal.id);
    setIsModalOpen(true);
    setError(null);
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this deal?')) return;
    if (!token) return;

    try {
      await apiDelete(`/api/deals/${id}`, token);
      setDeals(prevDeals => prevDeals.filter(deal => deal.id !== id));
      clearCache('/api/deals');
    } catch (err: any) {
      console.error('Error deleting deal:', err);
      setError(err.message || 'Failed to delete deal');
    }
  };

  const handleInputChange = (field: string, value: string | number) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    // Update stageName when stageId changes
    if (field === 'stageId') {
      const stage = stages.find(s => s.id === value);
      if (stage) {
        setFormData(prev => ({ ...prev, stageName: stage.name }));
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);
    
    if (!token) {
      setError('You must be logged in');
      setIsSubmitting(false);
      return;
    }

    try {
      const payload: any = {
        name: formData.name,
        accountId: formData.accountId || undefined,
        contactId: formData.contactId || undefined,
        amount: formData.amount ? parseFloat(formData.amount) : undefined,
        currency: formData.currency,
        stageId: formData.stageId,
        stageName: formData.stageName,
        probability: formData.probability ? parseFloat(formData.probability) : undefined,
        closeDate: formData.closeDate || undefined,
        status: formData.status,
        tags: formData.tags,
      };

      if (isEditMode && editingId) {
        await apiPut(`/api/deals/${editingId}`, token, payload);
      } else {
        await apiPost('/api/deals', token, payload);
      }

      setFormData({ 
        name: '', accountId: '', contactId: '', amount: '', currency: 'USD',
        stageId: 'prospecting', stageName: 'Prospecting', probability: '', 
        closeDate: '', status: 'open', tags: [] 
      });
      setIsModalOpen(false);
      setIsEditMode(false);
      setEditingId(null);
      
      clearCache('/api/deals');
      await fetchDeals();
    } catch (error: any) {
      console.error('Error saving deal:', error);
      setError(error.message || 'Failed to save deal');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    if (!isSubmitting) {
      setFormData({ 
        name: '', accountId: '', contactId: '', amount: '', currency: 'USD',
        stageId: 'prospecting', stageName: 'Prospecting', probability: '', 
        closeDate: '', status: 'open', tags: [] 
      });
      setIsModalOpen(false);
      setIsEditMode(false);
      setEditingId(null);
      setError(null);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading deals...</div>
      </div>
    );
  }

  if (error && !filteredDeals.length) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-red-600">{error}</div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Deals Pipeline</h1>
        <div className="flex items-center gap-2">
          <Input 
            placeholder="Search deals..." 
            value={query} 
            onChange={(e) => setQuery(e.target.value)}
            className="w-64"
          />
          <Button onClick={() => {
            setError(null);
            setIsEditMode(false);
            setEditingId(null);
            setIsModalOpen(true);
          }}>New Deal</Button>
        </div>
      </div>

      {error && (
        <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">
          {error}
        </div>
      )}

      <div className="flex gap-4 pb-4">
        {stages.map(stage => {
          const stageDeals = getDealsByStage(stage.id);
          return (
            <div
              key={stage.id}
              className={`flex-1 min-w-0 rounded-lg border-2 p-4 ${stage.color}`}
              onDragOver={handleDragOver}
              onDrop={(e) => handleDrop(e, stage.id)}
            >
              <div className="font-semibold mb-3 text-base text-gray-900 flex items-center justify-between">
                <span>{stage.name}</span>
                <span className="text-sm font-normal text-gray-600 bg-white px-2 py-0.5 rounded">
                  {stageDeals.length}
                </span>
              </div>
              <div className="flex flex-col gap-2 min-h-[150px]">
                {stageDeals.length === 0 ? (
                  <div className="text-center text-gray-400 text-xs py-6">
                    No deals
                  </div>
                ) : (
                  stageDeals.map(deal => (
                    <div
                      key={deal.id}
                      draggable
                      onDragStart={(e) => handleDragStart(e, deal)}
                      className="bg-white rounded-lg border border-gray-200 p-2.5 cursor-move hover:shadow-md transition-shadow"
                    >
                      <div className="flex items-start gap-2 mb-2">
                        <div className="w-7 h-7 rounded-full bg-brand-500 text-white flex items-center justify-center text-xs font-semibold flex-shrink-0">
                          {getInitials(deal.name)}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="font-medium text-sm text-gray-900 truncate">
                            {deal.name}
                          </div>
                          {deal.amount && (
                            <div className="text-xs font-semibold text-gray-700">
                              {deal.currency || 'USD'} {deal.amount.toLocaleString()}
                            </div>
                          )}
                        </div>
                      </div>

                      {deal.accountId && (
                        <div className="text-xs text-gray-600 mb-1 flex items-center gap-1">
                          <span>🏢</span>
                          <span className="truncate">{getAccountName(deal.accountId) || 'Unknown'}</span>
                        </div>
                      )}

                      <div className="flex items-center gap-2 mb-1.5 flex-wrap">
                        {deal.probability && (
                          <div className="text-xs text-gray-600">
                            <span className="font-medium">{deal.probability}%</span>
                          </div>
                        )}

                        {deal.closeDate && (
                          <div className="text-xs text-gray-600">
                            <span>📅</span> {new Date(deal.closeDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                          </div>
                        )}
                      </div>

                      {deal.tags && deal.tags.length > 0 && (
                        <div className="flex items-center gap-1 mb-1.5 flex-wrap">
                          {deal.tags.slice(0, 1).map((tag, idx) => (
                            <span key={idx} className="text-xs px-1.5 py-0.5 bg-blue-100 text-blue-700 rounded">
                              {tag}
                            </span>
                          ))}
                          {deal.tags.length > 1 && (
                            <span className="text-xs text-gray-500">+{deal.tags.length - 1}</span>
                          )}
                        </div>
                      )}

                      <div className="text-xs text-gray-400 mb-2">
                        {formatRelativeTime(deal.createdAt)}
                      </div>

                      <div className="flex gap-1.5 mt-1.5">
                        <button
                          onClick={() => handleEdit(deal)}
                          disabled={updatingStageId === deal.id}
                          className="flex-1 px-2 py-1 text-xs font-medium text-brand-600 bg-brand-50 hover:bg-brand-100 rounded disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleDelete(deal.id)}
                          disabled={updatingStageId === deal.id}
                          className="px-2 py-1 text-xs font-medium text-red-600 bg-red-50 hover:bg-red-100 rounded disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          );
        })}
      </div>

      <Modal 
        open={isModalOpen} 
        onClose={handleClose} 
        title={isEditMode ? "Edit Deal" : "Create New Deal"}
      >
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {error && (
            <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">
              {error}
            </div>
          )}
          
          <div className="flex flex-col gap-2">
            <label htmlFor="name" className="text-sm font-medium text-gray-700">
              Deal Name *
            </label>
            <Input
              id="name"
              type="text"
              placeholder="Enter deal name"
              value={formData.name}
              onChange={(e) => handleInputChange('name', e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <label htmlFor="accountId" className="text-sm font-medium text-gray-700">
                Account
              </label>
              <select
                id="accountId"
                className="h-9 px-3 rounded-md border border-gray-200 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500 shadow-sm"
                value={formData.accountId}
                onChange={(e) => handleInputChange('accountId', e.target.value)}
              >
                <option value="">No account</option>
                {accounts.map(account => (
                  <option key={account.id} value={account.id}>
                    {account.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="contactId" className="text-sm font-medium text-gray-700">
                Contact
              </label>
              <select
                id="contactId"
                className="h-9 px-3 rounded-md border border-gray-200 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500 shadow-sm"
                value={formData.contactId}
                onChange={(e) => handleInputChange('contactId', e.target.value)}
              >
                <option value="">No contact</option>
                {contacts.map(contact => (
                  <option key={contact.id} value={contact.id}>
                    {contact.firstName} {contact.lastName}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <label htmlFor="amount" className="text-sm font-medium text-gray-700">
                Amount
              </label>
              <Input
                id="amount"
                type="number"
                step="0.01"
                placeholder="0.00"
                value={formData.amount}
                onChange={(e) => handleInputChange('amount', e.target.value)}
              />
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="currency" className="text-sm font-medium text-gray-700">
                Currency
              </label>
              <select
                id="currency"
                className="h-9 px-3 rounded-md border border-gray-200 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500 shadow-sm"
                value={formData.currency}
                onChange={(e) => handleInputChange('currency', e.target.value)}
              >
                <option value="USD">USD</option>
                <option value="EUR">EUR</option>
                <option value="GBP">GBP</option>
                <option value="INR">INR</option>
              </select>
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="stageId" className="text-sm font-medium text-gray-700">
              Stage *
            </label>
            <select
              id="stageId"
              className="h-9 px-3 rounded-md border border-gray-200 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500 shadow-sm"
              value={formData.stageId}
              onChange={(e) => handleInputChange('stageId', e.target.value)}
              required
            >
              {stages.map(stage => (
                <option key={stage.id} value={stage.id}>
                  {stage.name}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <label htmlFor="probability" className="text-sm font-medium text-gray-700">
                Probability (%)
              </label>
              <Input
                id="probability"
                type="number"
                min="0"
                max="100"
                placeholder="0"
                value={formData.probability}
                onChange={(e) => handleInputChange('probability', e.target.value)}
              />
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="closeDate" className="text-sm font-medium text-gray-700">
                Close Date
              </label>
              <Input
                id="closeDate"
                type="date"
                value={formData.closeDate}
                onChange={(e) => handleInputChange('closeDate', e.target.value)}
              />
            </div>
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
              {isSubmitting ? 'Saving...' : isEditMode ? 'Update Deal' : 'Create Deal'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
