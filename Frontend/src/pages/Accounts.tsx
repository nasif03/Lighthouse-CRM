import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import { Table, THead, TBody, TR, TH, TD } from '../components/ui/Table';
import Modal from '../components/ui/Modal';
import { useState, useEffect, useCallback } from 'react';
import { useAuthStore } from '../store/authStore';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';

const API_BASE_URL = 'http://localhost:3000';

type Account = {
  id: string;
  name: string;
  domain?: string;
  industry?: string;
  phone?: string;
  status?: string;
  createdAt: string;
  updatedAt: string;
};

type Contact = {
  id: string;
  firstName: string;
  lastName?: string;
  email: string;
  accountId?: string;
};

type Deal = {
  id: string;
  name: string;
  amount?: number;
  currency?: string;
  accountId?: string;
  status: string;
};

export default function Accounts() {
  const { token } = useAuthStore();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const accountIdParam = searchParams.get('accountId');
  
  const [query, setQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [selectedAccount, setSelectedAccount] = useState<Account | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    domain: '',
    industry: '',
    phone: '',
    status: 'active',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [deals, setDeals] = useState<Deal[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAccounts = useCallback(async () => {
    if (!token) {
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const response = await fetch(`${API_BASE_URL}/api/accounts`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error('Failed to fetch accounts');
      }

      const data = await response.json();
      setAccounts(data);
      
      if (accountIdParam) {
        const account = data.find((a: Account) => a.id === accountIdParam);
        if (account) {
          setSelectedAccount(account);
        }
      }
    } catch (err: any) {
      console.error('Error fetching accounts:', err);
      setError(err.message || 'Failed to load accounts');
    } finally {
      setIsLoading(false);
    }
  }, [token, accountIdParam]);

  const fetchAccountDetails = useCallback(async (accountId: string) => {
    if (!token) return;

    try {
      const response = await fetch(`${API_BASE_URL}/api/accounts/${accountId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      
      if (response.ok) {
        const data = await response.json();
        setSelectedAccount(data.account);
        setContacts(data.contacts || []);
        setDeals(data.deals || []);
      } else {
        // Fallback to fetching all and filtering
        const contactsResponse = await fetch(`${API_BASE_URL}/api/contacts`, {
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        });
        if (contactsResponse.ok) {
          const allContacts = await contactsResponse.json();
          setContacts(allContacts.filter((c: Contact) => c.accountId === accountId));
        }

        const dealsResponse = await fetch(`${API_BASE_URL}/api/deals`, {
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        });
        if (dealsResponse.ok) {
          const allDeals = await dealsResponse.json();
          setDeals(allDeals.filter((d: Deal) => d.accountId === accountId));
        }
      }
    } catch (err) {
      console.error('Error fetching account details:', err);
    }
  }, [token]);

  useEffect(() => {
    if (token) {
      fetchAccounts();
      if (accountIdParam) {
        fetchAccountDetails(accountIdParam);
      }
    }
  }, [token, accountIdParam, fetchAccounts, fetchAccountDetails]);

  const filteredAccounts = accounts.filter(a => 
    a.name.toLowerCase().includes(query.toLowerCase()) ||
    a.domain?.toLowerCase().includes(query.toLowerCase()) ||
    a.industry?.toLowerCase().includes(query.toLowerCase())
  );

  const handleInputChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleEdit = (account: Account) => {
    setFormData({
      name: account.name,
      domain: account.domain || '',
      industry: account.industry || '',
      phone: account.phone || '',
      status: account.status || 'active',
    });
    setIsEditMode(true);
    setEditingId(account.id);
    setIsModalOpen(true);
    setError(null);
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this account?')) return;
    if (!token) return;

    try {
      const response = await fetch(`${API_BASE_URL}/api/accounts/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error('Failed to delete account');
      }

      await fetchAccounts();
    } catch (err: any) {
      console.error('Error deleting account:', err);
      alert(err.message || 'Failed to delete account');
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
        ? `${API_BASE_URL}/api/accounts/${editingId}`
        : `${API_BASE_URL}/api/accounts`;
      
      const method = isEditMode ? 'PUT' : 'POST';
      
      const response = await fetch(url, {
        method,
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      const responseData = await response.json().catch(() => null);

      if (!response.ok) {
        const errorMessage = responseData?.detail || responseData?.message || `HTTP ${response.status}`;
        throw new Error(errorMessage);
      }

      setFormData({ name: '', domain: '', industry: '', phone: '', status: 'active' });
      setIsModalOpen(false);
      setIsEditMode(false);
      setEditingId(null);
      
      await fetchAccounts();
    } catch (error: any) {
      console.error('Error saving account:', error);
      setError(error.message || 'Failed to save account');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    if (!isSubmitting) {
      setFormData({ name: '', domain: '', industry: '', phone: '', status: 'active' });
      setIsModalOpen(false);
      setIsEditMode(false);
      setEditingId(null);
      setError(null);
    }
  };

  if (selectedAccount && accountIdParam) {
    return (
      <div className="flex flex-col gap-4">
        <div className="flex items-center gap-2">
          <Button onClick={() => {
            setSelectedAccount(null);
            navigate('/accounts');
          }} variant="secondary">
            ← Back to Accounts
          </Button>
        </div>
        <Card>
          <CardHeader>{selectedAccount.name}</CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4 mb-6">
              <div>
                <div className="text-sm text-gray-600">Domain</div>
                <div className="font-medium">{selectedAccount.domain || '-'}</div>
              </div>
              <div>
                <div className="text-sm text-gray-600">Industry</div>
                <div className="font-medium">{selectedAccount.industry || '-'}</div>
              </div>
              <div>
                <div className="text-sm text-gray-600">Phone</div>
                <div className="font-medium">{selectedAccount.phone || '-'}</div>
              </div>
              <div>
                <div className="text-sm text-gray-600">Status</div>
                <div className="font-medium capitalize">{selectedAccount.status || '-'}</div>
              </div>
            </div>

            <div className="mb-6">
              <h3 className="text-lg font-semibold mb-3">Linked Contacts</h3>
              {contacts.length === 0 ? (
                <div className="text-gray-600">No contacts linked to this account</div>
              ) : (
                <Table>
                  <THead>
                    <tr>
                      <TH>Name</TH>
                      <TH>Email</TH>
                      <TH>Actions</TH>
                    </tr>
                  </THead>
                  <TBody>
                    {contacts.map(c => (
                      <TR key={c.id}>
                        <TD>{c.firstName} {c.lastName}</TD>
                        <TD>{c.email}</TD>
                        <TD>
                          <Link to="/contacts" className="text-brand-600 hover:underline text-sm">
                            View
                          </Link>
                        </TD>
                      </TR>
                    ))}
                  </TBody>
                </Table>
              )}
            </div>

            <div>
              <h3 className="text-lg font-semibold mb-3">Linked Deals</h3>
              {deals.length === 0 ? (
                <div className="text-gray-600">No deals linked to this account</div>
              ) : (
                <Table>
                  <THead>
                    <tr>
                      <TH>Name</TH>
                      <TH>Amount</TH>
                      <TH>Status</TH>
                      <TH>Actions</TH>
                    </tr>
                  </THead>
                  <TBody>
                    {deals.map(d => (
                      <TR key={d.id}>
                        <TD>{d.name}</TD>
                        <TD>
                          {d.amount ? `${d.currency || 'USD'} ${d.amount.toLocaleString()}` : '-'}
                        </TD>
                        <TD className="capitalize">{d.status}</TD>
                        <TD>
                          <Link to="/deals" className="text-brand-600 hover:underline text-sm">
                            View
                          </Link>
                        </TD>
                      </TR>
                    ))}
                  </TBody>
                </Table>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-2">
        <Input 
          placeholder="Search accounts" 
          value={query} 
          onChange={(e) => setQuery(e.target.value)} 
        />
        <Button onClick={() => {
          setError(null);
          setIsEditMode(false);
          setEditingId(null);
          setIsModalOpen(true);
        }}>New Account</Button>
      </div>
      <Card>
        <CardHeader>Accounts</CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-gray-600">Loading accounts...</div>
          ) : error ? (
            <div className="text-center py-8 text-red-600">{error}</div>
          ) : filteredAccounts.length === 0 ? (
            <div className="text-center py-8 text-gray-600">
              {query ? 'No accounts found matching your search' : 'No accounts yet. Create your first account!'}
            </div>
          ) : (
            <Table>
              <THead>
                <tr>
                  <TH>Name</TH>
                  <TH>Domain</TH>
                  <TH>Industry</TH>
                  <TH>Phone</TH>
                  <TH>Status</TH>
                  <TH>Actions</TH>
                </tr>
              </THead>
              <TBody>
                {filteredAccounts.map(a => (
                  <TR key={a.id}>
                    <TD>
                      <Link 
                        to={`/accounts?accountId=${a.id}`}
                        className="text-brand-600 hover:underline"
                      >
                        {a.name}
                      </Link>
                    </TD>
                    <TD>{a.domain || '-'}</TD>
                    <TD>{a.industry || '-'}</TD>
                    <TD>{a.phone || '-'}</TD>
                    <TD className="capitalize">{a.status || '-'}</TD>
                    <TD>
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleEdit(a)}
                          className="text-sm text-brand-600 hover:underline"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleDelete(a.id)}
                          className="text-sm text-red-600 hover:underline"
                        >
                          Delete
                        </button>
                      </div>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Modal 
        open={isModalOpen} 
        onClose={handleClose} 
        title={isEditMode ? "Edit Account" : "Create New Account"}
      >
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {error && (
            <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">
              {error}
            </div>
          )}
          
          <div className="flex flex-col gap-2">
            <label htmlFor="name" className="text-sm font-medium text-gray-700">
              Name *
            </label>
            <Input
              id="name"
              type="text"
              placeholder="Enter account name"
              value={formData.name}
              onChange={(e) => handleInputChange('name', e.target.value)}
              required
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="domain" className="text-sm font-medium text-gray-700">
              Domain
            </label>
            <Input
              id="domain"
              type="text"
              placeholder="Enter domain"
              value={formData.domain}
              onChange={(e) => handleInputChange('domain', e.target.value)}
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="industry" className="text-sm font-medium text-gray-700">
              Industry
            </label>
            <Input
              id="industry"
              type="text"
              placeholder="Enter industry"
              value={formData.industry}
              onChange={(e) => handleInputChange('industry', e.target.value)}
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="phone" className="text-sm font-medium text-gray-700">
              Phone
            </label>
            <Input
              id="phone"
              type="tel"
              placeholder="Enter phone number"
              value={formData.phone}
              onChange={(e) => handleInputChange('phone', e.target.value)}
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
            >
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
              <option value="pending">Pending</option>
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
              {isSubmitting ? 'Saving...' : isEditMode ? 'Update Account' : 'Create Account'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

