import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Modal from '../components/ui/Modal';
import { useState, useEffect, useCallback } from 'react';
import { useAuthStore } from '../store/authStore';

const API_BASE_URL = 'http://localhost:3000';

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

// Default sales stages
const DEFAULT_STAGES = [
  { id: 'prospecting', name: 'Prospecting', order: 0 },
  { id: 'qualification', name: 'Qualification', order: 1 },
  { id: 'proposal', name: 'Proposal', order: 2 },
  { id: 'negotiation', name: 'Negotiation', order: 3 },
  { id: 'closed-won', name: 'Closed Won', order: 4 },
  { id: 'closed-lost', name: 'Closed Lost', order: 5 },
];

export default function Deals() {
  const { token } = useAuthStore();
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

  const fetchDeals = useCallback(async () => {
    if (!token) {
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const response = await fetch(`${API_BASE_URL}/api/deals`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error('Failed to fetch deals');
      }

      const data = await response.json();
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
      const response = await fetch(`${API_BASE_URL}/api/accounts`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        setAccounts(data);
      }
    } catch (err) {
      console.error('Error fetching accounts:', err);
    }
  }, [token]);

  const fetchContacts = useCallback(async () => {
    if (!token) return;

    try {
      const response = await fetch(`${API_BASE_URL}/api/contacts`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        setContacts(data);
      }
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

  const getDealsByStage = (stageId: string) => {
    return deals.filter(d => (d.stageId || 'prospecting') === stageId);
  };

  const handleDragStart = (e: React.DragEvent, deal: Deal) => {
    setDraggedDeal(deal);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  };

  const handleDrop = async (e: React.DragEvent, targetStageId: string) => {
    e.preventDefault();
    if (!draggedDeal || !token) return;

    const targetStage = stages.find(s => s.id === targetStageId);
    if (!targetStage) return;

    // Update deal stage
    try {
      const response = await fetch(`${API_BASE_URL}/api/deals/${draggedDeal.id}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          stageId: targetStageId,
          stageName: targetStage.name,
        }),
      });

      if (!response.ok) {
        throw new Error('Failed to update deal stage');
      }

      await fetchDeals();
    } catch (err: any) {
      console.error('Error updating deal stage:', err);
      alert(err.message || 'Failed to update deal stage');
    }

    setDraggedDeal(null);
  };

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
      const response = await fetch(`${API_BASE_URL}/api/deals/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error('Failed to delete deal');
      }

      await fetchDeals();
    } catch (err: any) {
      console.error('Error deleting deal:', err);
      alert(err.message || 'Failed to delete deal');
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
      const url = isEditMode && editingId
        ? `${API_BASE_URL}/api/deals/${editingId}`
        : `${API_BASE_URL}/api/deals`;
      
      const method = isEditMode ? 'PUT' : 'POST';
      
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

      const response = await fetch(url, {
        method,
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      });

      const responseData = await response.json().catch(() => null);

      if (!response.ok) {
        const errorMessage = responseData?.detail || responseData?.message || `HTTP ${response.status}`;
        throw new Error(errorMessage);
      }

      setFormData({ 
        name: '', accountId: '', contactId: '', amount: '', currency: 'USD',
        stageId: 'prospecting', stageName: 'Prospecting', probability: '', 
        closeDate: '', status: 'open', tags: [] 
      });
      setIsModalOpen(false);
      setIsEditMode(false);
      setEditingId(null);
      
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

  const getAccountName = (accountId?: string) => {
    if (!accountId) return null;
    const account = accounts.find(a => a.id === accountId);
    return account?.name;
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading deals...</div>
      </div>
    );
  }

  if (error) {
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
        <Button onClick={() => {
          setError(null);
          setIsEditMode(false);
          setEditingId(null);
          setIsModalOpen(true);
        }}>New Deal</Button>
      </div>

      <div className="flex gap-4 overflow-x-auto pb-4">
        {stages.map(stage => {
          const stageDeals = getDealsByStage(stage.id);
          return (
            <div
              key={stage.id}
              className="flex-shrink-0 w-80 bg-gray-50 rounded-lg border border-gray-200 p-4"
              onDragOver={handleDragOver}
              onDrop={(e) => handleDrop(e, stage.id)}
            >
              <div className="font-semibold mb-3 text-gray-900">
                {stage.name} ({stageDeals.length})
              </div>
              <div className="flex flex-col gap-2 min-h-[200px]">
                {stageDeals.map(deal => (
                  <div
                    key={deal.id}
                    draggable
                    onDragStart={(e) => handleDragStart(e, deal)}
                    className="bg-white rounded border border-gray-200 p-3 cursor-move hover:shadow-md transition-shadow"
                  >
                    <div className="font-medium text-sm mb-1">{deal.name}</div>
                    {deal.amount && (
                      <div className="text-xs text-gray-600 mb-1">
                        {deal.currency || 'USD'} {deal.amount.toLocaleString()}
                      </div>
                    )}
                    {deal.accountId && (
                      <div className="text-xs text-gray-500 mb-1">
                        Account: {getAccountName(deal.accountId) || 'Unknown'}
                      </div>
                    )}
                    {deal.probability && (
                      <div className="text-xs text-gray-500 mb-2">
                        {deal.probability}% probability
                      </div>
                    )}
                    <div className="flex gap-2 mt-2">
                      <button
                        onClick={() => handleEdit(deal)}
                        className="text-xs text-brand-600 hover:underline"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(deal.id)}
                        className="text-xs text-red-600 hover:underline"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                ))}
                {stageDeals.length === 0 && (
                  <div className="text-sm text-gray-400 text-center py-8">
                    Drop deals here
                  </div>
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
