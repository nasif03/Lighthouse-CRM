import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';

const API_BASE_URL = 'http://localhost:3000';

type TicketFormData = {
  name: string;
  email: string;
  phone: string;
  subject: string;
  description: string;
  priority: string;
  category: string;
};

export default function SubmitTicket() {
  const { orgId } = useParams<{ orgId: string }>();
  const [formData, setFormData] = useState<TicketFormData>({
    name: '',
    email: '',
    phone: '',
    subject: '',
    description: '',
    priority: 'medium',
    category: '',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [ticketNumber, setTicketNumber] = useState<string | null>(null);

  useEffect(() => {
    if (!orgId) {
      setError('Organization ID is required');
    }
  }, [orgId]);

  const handleInputChange = (field: keyof TicketFormData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    setError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!orgId) {
      setError('Organization ID is required');
      return;
    }

    if (!formData.name || !formData.email || !formData.subject || !formData.description) {
      setError('Please fill in all required fields');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const response = await fetch(`${API_BASE_URL}/api/tickets`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          orgId,
          ...formData,
        }),
      });

      const responseData = await response.json().catch(() => null);

      if (!response.ok) {
        const errorMessage = responseData?.detail || responseData?.message || `HTTP ${response.status}`;
        throw new Error(errorMessage);
      }

      setTicketNumber(responseData.ticketNumber);
      setSuccess(true);
      setFormData({
        name: '',
        email: '',
        phone: '',
        subject: '',
        description: '',
        priority: 'medium',
        category: '',
      });
    } catch (error: any) {
      console.error('Error submitting ticket:', error);
      setError(error.message || 'Failed to submit ticket. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-brand-50 to-gray-100 flex items-center justify-center p-4">
        <Card className="max-w-md w-full">
          <CardContent className="p-8 text-center">
            <div className="mb-6">
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <h1 className="text-2xl font-bold text-gray-900 mb-2">Ticket Submitted Successfully!</h1>
              <p className="text-gray-600 mb-4">
                Your support ticket has been created and our team will get back to you soon.
              </p>
              {ticketNumber && (
                <div className="bg-brand-50 border border-brand-200 rounded-lg p-4 mb-4">
                  <p className="text-sm text-gray-600 mb-1">Your Ticket Number:</p>
                  <p className="text-xl font-bold text-brand-700">{ticketNumber}</p>
                  <p className="text-xs text-gray-500 mt-2">
                    Please save this number for future reference
                  </p>
                </div>
              )}
            </div>
            <Button
              onClick={() => {
                setSuccess(false);
                setTicketNumber(null);
              }}
              className="w-full"
            >
              Submit Another Ticket
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-brand-50 to-gray-100 flex items-center justify-center p-4">
      <Card className="max-w-2xl w-full">
        <CardHeader>
          <h1 className="text-2xl font-bold text-gray-900">Submit a Support Ticket</h1>
          <p className="text-sm text-gray-600 mt-1">
            Fill out the form below and we'll get back to you as soon as possible.
          </p>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            {error && (
              <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">
                {error}
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex flex-col gap-2">
                <label htmlFor="name" className="text-sm font-medium text-gray-700">
                  Full Name *
                </label>
                <Input
                  id="name"
                  type="text"
                  placeholder="Enter your full name"
                  value={formData.name}
                  onChange={(e) => handleInputChange('name', e.target.value)}
                  required
                />
              </div>

              <div className="flex flex-col gap-2">
                <label htmlFor="email" className="text-sm font-medium text-gray-700">
                  Email Address *
                </label>
                <Input
                  id="email"
                  type="email"
                  placeholder="Enter your email"
                  value={formData.email}
                  onChange={(e) => handleInputChange('email', e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex flex-col gap-2">
                <label htmlFor="phone" className="text-sm font-medium text-gray-700">
                  Phone Number
                </label>
                <Input
                  id="phone"
                  type="tel"
                  placeholder="Enter your phone number"
                  value={formData.phone}
                  onChange={(e) => handleInputChange('phone', e.target.value)}
                />
              </div>

              <div className="flex flex-col gap-2">
                <label htmlFor="priority" className="text-sm font-medium text-gray-700">
                  Priority
                </label>
                <select
                  id="priority"
                  className="h-9 px-3 rounded-md border border-gray-200 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500 shadow-sm"
                  value={formData.priority}
                  onChange={(e) => handleInputChange('priority', e.target.value)}
                >
                  <option value="low">Low</option>
                  <option value="medium">Medium</option>
                  <option value="high">High</option>
                  <option value="urgent">Urgent</option>
                </select>
              </div>
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="category" className="text-sm font-medium text-gray-700">
                Category
              </label>
              <select
                id="category"
                className="h-9 px-3 rounded-md border border-gray-200 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500 shadow-sm"
                value={formData.category}
                onChange={(e) => handleInputChange('category', e.target.value)}
              >
                <option value="">Select a category</option>
                <option value="technical">Technical Issue</option>
                <option value="billing">Billing</option>
                <option value="feature_request">Feature Request</option>
                <option value="bug_report">Bug Report</option>
                <option value="account">Account Issue</option>
                <option value="other">Other</option>
              </select>
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="subject" className="text-sm font-medium text-gray-700">
                Subject *
              </label>
              <Input
                id="subject"
                type="text"
                placeholder="Brief description of your issue"
                value={formData.subject}
                onChange={(e) => handleInputChange('subject', e.target.value)}
                required
              />
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="description" className="text-sm font-medium text-gray-700">
                Description *
              </label>
              <textarea
                id="description"
                rows={6}
                className="px-3 py-2 rounded-md border border-gray-200 bg-white text-sm outline-none focus:ring-2 focus:ring-brand-500 shadow-sm resize-none"
                placeholder="Please provide detailed information about your issue..."
                value={formData.description}
                onChange={(e) => handleInputChange('description', e.target.value)}
                required
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <Button type="submit" disabled={isSubmitting} className="w-full md:w-auto">
                {isSubmitting ? 'Submitting...' : 'Submit Ticket'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

