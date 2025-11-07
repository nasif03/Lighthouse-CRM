import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Textarea from '../components/ui/Textarea';
import Select from '../components/ui/Select';
import FileUpload from '../components/ui/FileUpload';
import { useAuthStore } from '../store/authStore';

type TicketFormData = {
  subject: string;
  description: string;
  category: string;
  priority: string;
  contactName: string;
  contactEmail: string;
  contactPhone: string;
  attachments: File[];
};

export default function CreateTicket() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [formData, setFormData] = useState<TicketFormData>({
    subject: '',
    description: '',
    category: '',
    priority: 'Medium',
    contactName: user?.name || '',
    contactEmail: user?.email || '',
    contactPhone: '',
    attachments: [],
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string>('');

  const handleChange = (field: keyof TicketFormData, value: string | File[]) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    // Validation
    if (!formData.subject.trim()) {
      setError('Subject is required');
      return;
    }
    if (!formData.description.trim()) {
      setError('Description is required');
      return;
    }
    if (!formData.category) {
      setError('Category is required');
      return;
    }
    if (!formData.contactEmail.trim()) {
      setError('Email is required');
      return;
    }

    setIsSubmitting(true);

    try {
      // TODO: Replace with actual API call
      // const formDataToSend = new FormData();
      // Object.entries(formData).forEach(([key, value]) => {
      //   if (key === 'attachments') {
      //     formData.attachments.forEach(file => formDataToSend.append('files', file));
      //   } else {
      //     formDataToSend.append(key, value as string);
      //   }
      // });
      // const response = await fetch('/api/tickets', { method: 'POST', body: formDataToSend });

      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Navigate to ticket detail or success page
      navigate('/support', { state: { message: 'Ticket created successfully!' } });
    } catch (err) {
      setError('Failed to create ticket. Please try again.');
      console.error(err);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto">
      <Card>
        <CardHeader>
          <h1 className="text-2xl font-semibold">Create Support Ticket</h1>
          <p className="text-sm text-gray-500 mt-1">Fill out the form below to submit a support request</p>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
                {error}
              </div>
            )}

            {/* Subject */}
            <div>
              <label htmlFor="subject" className="block text-sm font-medium text-gray-700 mb-1">
                Subject / Title <span className="text-red-500">*</span>
              </label>
              <Input
                id="subject"
                value={formData.subject}
                onChange={(e) => handleChange('subject', e.target.value)}
                placeholder="Brief summary of your issue"
                required
              />
            </div>

            {/* Description */}
            <div>
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                Description / Details <span className="text-red-500">*</span>
              </label>
              <Textarea
                id="description"
                value={formData.description}
                onChange={(e) => handleChange('description', e.target.value)}
                placeholder="Please provide detailed information about your issue..."
                rows={6}
                required
              />
            </div>

            {/* Category and Priority Row */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="category" className="block text-sm font-medium text-gray-700 mb-1">
                  Category / Type <span className="text-red-500">*</span>
                </label>
                <Select
                  id="category"
                  value={formData.category}
                  onChange={(e) => handleChange('category', e.target.value)}
                  required
                >
                  <option value="">Select category</option>
                  <option value="Technical">Technical</option>
                  <option value="Billing">Billing</option>
                  <option value="Account">Account</option>
                  <option value="Feature Request">Feature Request</option>
                  <option value="Bug Report">Bug Report</option>
                  <option value="Feedback">Feedback</option>
                  <option value="Other">Other</option>
                </Select>
              </div>

              <div>
                <label htmlFor="priority" className="block text-sm font-medium text-gray-700 mb-1">
                  Priority / Severity
                </label>
                <Select
                  id="priority"
                  value={formData.priority}
                  onChange={(e) => handleChange('priority', e.target.value)}
                >
                  <option value="Low">Low</option>
                  <option value="Medium">Medium</option>
                  <option value="High">High</option>
                  <option value="Critical">Critical</option>
                </Select>
              </div>
            </div>

            {/* Contact Information */}
            <div className="border-t pt-6">
              <h3 className="text-sm font-semibold text-gray-900 mb-4">Contact Information</h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label htmlFor="contactName" className="block text-sm font-medium text-gray-700 mb-1">
                    Name <span className="text-red-500">*</span>
                  </label>
                  <Input
                    id="contactName"
                    value={formData.contactName}
                    onChange={(e) => handleChange('contactName', e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label htmlFor="contactEmail" className="block text-sm font-medium text-gray-700 mb-1">
                    Email <span className="text-red-500">*</span>
                  </label>
                  <Input
                    id="contactEmail"
                    type="email"
                    value={formData.contactEmail}
                    onChange={(e) => handleChange('contactEmail', e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label htmlFor="contactPhone" className="block text-sm font-medium text-gray-700 mb-1">
                    Phone
                  </label>
                  <Input
                    id="contactPhone"
                    type="tel"
                    value={formData.contactPhone}
                    onChange={(e) => handleChange('contactPhone', e.target.value)}
                    placeholder="Optional"
                  />
                </div>
              </div>
            </div>

            {/* Attachments */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Attachments
              </label>
              <FileUpload
                accept="image/*,.pdf,.doc,.docx,.txt,.log"
                multiple
                maxSizeMB={10}
                onChange={(files) => handleChange('attachments', files)}
              />
              <p className="mt-1 text-xs text-gray-500">
                Upload screenshots, logs, or documents that may help us understand your issue
              </p>
            </div>

            {/* Submit Buttons */}
            <div className="flex items-center justify-end gap-3 pt-4 border-t">
              <Button
                type="button"
                variant="secondary"
                onClick={() => navigate('/support')}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting}
              >
                {isSubmitting ? 'Submitting...' : 'Submit Ticket'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

