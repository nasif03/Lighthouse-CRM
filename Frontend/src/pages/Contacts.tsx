import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Modal from '../components/ui/Modal';
import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useAuthStore } from '../store/authStore';
import { apiGet, apiPost, apiPut, apiDelete, clearCache } from '../utils/api';
import { useDebounce } from '../hooks/useDebounce';
import { Link } from 'react-router-dom';

type Contact = {
  id: string;
  firstName: string;
  lastName?: string;
  email: string;
  phone?: string;
  title?: string;
  accountId?: string;
  tags: string[];
  createdAt: string;
  updatedAt: string;
};

type Account = {
  id: string;
  name: string;
};

// Helper function to get initials from name
const getInitials = (firstName: string, lastName?: string): string => {
  if (lastName) {
    return (firstName[0] + lastName[0]).toUpperCase();
  }
  return firstName.substring(0, 2).toUpperCase();
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

export default function Contacts() {
  const { token } = useAuthStore();
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebounce(query, 300);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    title: '',
    accountId: '',
    tags: [] as string[],
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const fetchingRef = useRef(false);

  const fetchContacts = useCallback(async () => {
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
      const data = await apiGet<Contact[]>('/api/contacts', token);
      setContacts(data);
    } catch (err: any) {
      if (err.message === 'Request cancelled') {
        return;
      }
      console.error('Error fetching contacts:', err);
      setError(err.message || 'Failed to load contacts');
    } finally {
      setIsLoading(false);
      fetchingRef.current = false;
    }
  }, [token]);

  const fetchAccounts = useCallback(async () => {
    if (!token) return;

    try {
      const data = await apiGet<Account[]>('/api/accounts', token);
      setAccounts(data);
    } catch (err: any) {
      if (err.message === 'Request cancelled') {
        return;
      }
      console.error('Error fetching accounts:', err);
    }
  }, [token]);

  useEffect(() => {
    if (token) {
      fetchContacts();
      fetchAccounts();
    }
  }, [token, fetchContacts, fetchAccounts]);

  const getAccountName = (accountId?: string) => {
    if (!accountId) return null;
    const account = accounts.find(a => a.id === accountId);
    return account?.name;
  };

  const filteredContacts = useMemo(() => {
    if (!debouncedQuery) return contacts;
    const lowerQuery = debouncedQuery.toLowerCase();
    return contacts.filter(c => {
      const fullName = `${c.firstName} ${c.lastName || ''}`.toLowerCase();
      const accountName = c.accountId ? getAccountName(c.accountId)?.toLowerCase() : null;
      return (
        fullName.includes(lowerQuery) ||
        c.email.toLowerCase().includes(lowerQuery) ||
        (c.phone && c.phone.toLowerCase().includes(lowerQuery)) ||
        (accountName && accountName.includes(lowerQuery))
      );
    });
  }, [contacts, debouncedQuery, accounts]);

  const handleInputChange = (field: string, value: string | string[]) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleEdit = (contact: Contact) => {
    setFormData({
      firstName: contact.firstName,
      lastName: contact.lastName || '',
      email: contact.email,
      phone: contact.phone || '',
      title: contact.title || '',
      accountId: contact.accountId || '',
      tags: contact.tags || [],
    });
    setIsEditMode(true);
    setEditingId(contact.id);
    setIsModalOpen(true);
    setError(null);
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this contact?')) return;
    if (!token) return;

    try {
      await apiDelete(`/api/contacts/${id}`, token);
      setContacts(prevContacts => prevContacts.filter(contact => contact.id !== id));
      clearCache('/api/contacts');
    } catch (err: any) {
      console.error('Error deleting contact:', err);
      setError(err.message || 'Failed to delete contact');
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
        firstName: formData.firstName,
        lastName: formData.lastName || undefined,
        email: formData.email,
        phone: formData.phone || undefined,
        title: formData.title || undefined,
        accountId: formData.accountId || undefined,
        tags: formData.tags,
      };

      if (isEditMode && editingId) {
        await apiPut(`/api/contacts/${editingId}`, token, payload);
      } else {
        await apiPost('/api/contacts', token, payload);
      }

      setFormData({ firstName: '', lastName: '', email: '', phone: '', title: '', accountId: '', tags: [] });
      setIsModalOpen(false);
      setIsEditMode(false);
      setEditingId(null);
      
      clearCache('/api/contacts');
      await fetchContacts();
    } catch (error: any) {
      console.error('Error saving contact:', error);
      setError(error.message || 'Failed to save contact');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    if (!isSubmitting) {
      setFormData({ firstName: '', lastName: '', email: '', phone: '', title: '', accountId: '', tags: [] });
      setIsModalOpen(false);
      setIsEditMode(false);
      setEditingId(null);
      setError(null);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading contacts...</div>
      </div>
    );
  }

  if (error && !filteredContacts.length) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-red-600">{error}</div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Contacts</h1>
        <div className="flex items-center gap-2">
          <Input 
            placeholder="Search contacts..." 
            value={query} 
            onChange={(e) => setQuery(e.target.value)}
            className="w-64"
          />
          <Button onClick={() => {
            setError(null);
            setIsEditMode(false);
            setEditingId(null);
            setIsModalOpen(true);
          }}>New Contact</Button>
        </div>
      </div>

      {error && (
        <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">
          {error}
        </div>
      )}

      {filteredContacts.length === 0 ? (
        <div className="text-center py-12 text-gray-600">
          {query ? 'No contacts found matching your search' : 'No contacts yet. Create your first contact!'}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {filteredContacts.map(contact => {
            const fullName = `${contact.firstName} ${contact.lastName || ''}`.trim();
            return (
              <div
                key={contact.id}
                className="bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow"
              >
                <div className="flex items-start gap-3 mb-3">
                  <div className="w-10 h-10 rounded-full bg-brand-500 text-white flex items-center justify-center text-sm font-semibold flex-shrink-0">
                    {getInitials(contact.firstName, contact.lastName)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="font-medium text-base text-gray-900 truncate">
                      {fullName}
                    </div>
                    {contact.title && (
                      <div className="text-sm text-gray-600 truncate">
                        {contact.title}
                      </div>
                    )}
                  </div>
                </div>

                <div className="space-y-1.5 mb-3">
                  {contact.email && (
                    <div className="text-sm text-gray-600 truncate" title={contact.email}>
                      📧 {contact.email}
                    </div>
                  )}
                  {contact.phone && (
                    <div className="text-sm text-gray-600">
                      📞 {contact.phone}
                    </div>
                  )}
                  {contact.accountId && (
                    <div className="text-sm text-gray-600 truncate">
                      <Link 
                        to={`/accounts?accountId=${contact.accountId}`} 
                        className="text-brand-600 hover:underline flex items-center gap-1"
                      >
                        🏢 {getAccountName(contact.accountId) || 'View Account'}
                      </Link>
                    </div>
                  )}
                </div>

                {contact.tags && contact.tags.length > 0 && (
                  <div className="flex items-center gap-1 mb-3 flex-wrap">
                    {contact.tags.slice(0, 2).map((tag, idx) => (
                      <span key={idx} className="text-xs px-2 py-0.5 bg-blue-100 text-blue-700 rounded">
                        {tag}
                      </span>
                    ))}
                    {contact.tags.length > 2 && (
                      <span className="text-xs text-gray-500">+{contact.tags.length - 2}</span>
                    )}
                  </div>
                )}

                <div className="text-xs text-gray-400 mb-3">
                  {formatRelativeTime(contact.createdAt)}
                </div>

                <div className="flex gap-2 pt-3 border-t border-gray-100">
                  <button
                    onClick={() => handleEdit(contact)}
                    className="flex-1 px-3 py-1.5 text-sm font-medium text-brand-600 bg-brand-50 hover:bg-brand-100 rounded transition-colors"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(contact.id)}
                    className="px-3 py-1.5 text-sm font-medium text-red-600 bg-red-50 hover:bg-red-100 rounded transition-colors"
                  >
                    Delete
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <Modal 
        open={isModalOpen} 
        onClose={handleClose} 
        title={isEditMode ? "Edit Contact" : "Create New Contact"}
      >
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {error && (
            <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">
              {error}
            </div>
          )}
          
          <div className="flex flex-col gap-2">
            <label htmlFor="firstName" className="text-sm font-medium text-gray-700">
              First Name *
            </label>
            <Input
              id="firstName"
              type="text"
              placeholder="Enter first name"
              value={formData.firstName}
              onChange={(e) => handleInputChange('firstName', e.target.value)}
              required
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="lastName" className="text-sm font-medium text-gray-700">
              Last Name
            </label>
            <Input
              id="lastName"
              type="text"
              placeholder="Enter last name"
              value={formData.lastName}
              onChange={(e) => handleInputChange('lastName', e.target.value)}
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="email" className="text-sm font-medium text-gray-700">
              Email *
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
            <label htmlFor="title" className="text-sm font-medium text-gray-700">
              Title
            </label>
            <Input
              id="title"
              type="text"
              placeholder="Enter job title"
              value={formData.title}
              onChange={(e) => handleInputChange('title', e.target.value)}
            />
          </div>

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
              {isSubmitting ? 'Saving...' : isEditMode ? 'Update Contact' : 'Create Contact'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
