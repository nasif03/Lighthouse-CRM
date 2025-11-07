import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import { Table, THead, TBody, TR, TH, TD } from '../components/ui/Table';
import Modal from '../components/ui/Modal';
import { useState, useEffect, useCallback } from 'react';
import { useAuthStore } from '../store/authStore';
import { Link } from 'react-router-dom';

const API_BASE_URL = 'http://localhost:3000';

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

export default function Contacts() {
  const { token } = useAuthStore();
  const [query, setQuery] = useState('');
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

  const fetchContacts = useCallback(async () => {
    if (!token) {
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const response = await fetch(`${API_BASE_URL}/api/contacts`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error('Failed to fetch contacts');
      }

      const data = await response.json();
      setContacts(data);
    } catch (err: any) {
      console.error('Error fetching contacts:', err);
      setError(err.message || 'Failed to load contacts');
    } finally {
      setIsLoading(false);
    }
  }, [token]);

  const fetchAccounts = useCallback(async () => {
    if (!token) return;

    try {
      const response = await fetch(`${API_BASE_URL}/api/accounts`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
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

  useEffect(() => {
    if (token) {
      fetchContacts();
      fetchAccounts();
    }
  }, [token, fetchContacts, fetchAccounts]);

  const filteredContacts = contacts.filter(c => 
    c.firstName.toLowerCase().includes(query.toLowerCase()) ||
    c.lastName?.toLowerCase().includes(query.toLowerCase()) ||
    c.email.toLowerCase().includes(query.toLowerCase())
  );

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
      const response = await fetch(`${API_BASE_URL}/api/contacts/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error('Failed to delete contact');
      }

      await fetchContacts();
    } catch (err: any) {
      console.error('Error deleting contact:', err);
      alert(err.message || 'Failed to delete contact');
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
        ? `${API_BASE_URL}/api/contacts/${editingId}`
        : `${API_BASE_URL}/api/contacts`;
      
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

      setFormData({ firstName: '', lastName: '', email: '', phone: '', title: '', accountId: '', tags: [] });
      setIsModalOpen(false);
      setIsEditMode(false);
      setEditingId(null);
      
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

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-2">
        <Input 
          placeholder="Search contacts" 
          value={query} 
          onChange={(e) => setQuery(e.target.value)} 
        />
        <Button onClick={() => {
          setError(null);
          setIsEditMode(false);
          setEditingId(null);
          setIsModalOpen(true);
        }}>New Contact</Button>
      </div>
      <Card>
        <CardHeader>Contacts</CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-gray-600">Loading contacts...</div>
          ) : error ? (
            <div className="text-center py-8 text-red-600">{error}</div>
          ) : filteredContacts.length === 0 ? (
            <div className="text-center py-8 text-gray-600">
              {query ? 'No contacts found matching your search' : 'No contacts yet. Create your first contact!'}
            </div>
          ) : (
            <Table>
              <THead>
                <tr>
                  <TH>Name</TH>
                  <TH>Email</TH>
                  <TH>Phone</TH>
                  <TH>Title</TH>
                  <TH>Account</TH>
                  <TH>Actions</TH>
                </tr>
              </THead>
              <TBody>
                {filteredContacts.map(c => (
                  <TR key={c.id}>
                    <TD>{c.firstName} {c.lastName}</TD>
                    <TD>{c.email}</TD>
                    <TD>{c.phone || '-'}</TD>
                    <TD>{c.title || '-'}</TD>
                    <TD>
                      {c.accountId ? (
                        <Link to={`/accounts?accountId=${c.accountId}`} className="text-brand-600 hover:underline">
                          View Account
                        </Link>
                      ) : '-'}
                    </TD>
                    <TD>
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleEdit(c)}
                          className="text-sm text-brand-600 hover:underline"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleDelete(c.id)}
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
