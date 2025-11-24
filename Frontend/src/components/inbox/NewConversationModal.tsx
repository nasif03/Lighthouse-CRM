import { useState, useEffect } from 'react';
import { useAuthStore } from '../../store/authStore';
import { useInboxStore } from '../../store/inboxStore';
import { apiGet, apiPost } from '../../utils/api';
import Button from '../ui/Button';
import Input from '../ui/Input';

type Employee = {
  id?: string;  // API might return 'id'
  _id?: string; // API might return '_id'
  name: string;
  email: string;
  picture?: string;
};

type Props = {
  isOpen: boolean;
  onClose: () => void;
};

export default function NewConversationModal({ isOpen, onClose }: Props) {
  const { token, user } = useAuthStore();
  const { createDirectChannel, fetchConversations, setActiveConversation } = useInboxStore();
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  useEffect(() => {
    if (isOpen && token) {
      loadEmployees();
    }
  }, [isOpen, token]);

  const loadEmployees = async () => {
    if (!token) return;
    
    setIsLoading(true);
    try {
      // Use chat users endpoint which is available to all authenticated users
      const data = await apiGet<Employee[]>(
        `/api/chat/users`,
        token
      );
      setEmployees(data);
    } catch (error: any) {
      console.error('Failed to load employees:', error);
      setEmployees([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateConversation = async (otherUserId: string) => {
    if (!token) return;

    console.log('handleCreateConversation called with user_id:', otherUserId);
    setIsCreating(true);
    try {
      const conversation = await createDirectChannel(token, otherUserId);
      if (conversation && conversation.id) {
        // Refresh conversations list to get full data
        await fetchConversations(token);
        
        // Wait a moment for state to update
        await new Promise(resolve => setTimeout(resolve, 200));
        
        // Open the new conversation using the ID
        setActiveConversation(conversation.id);
        onClose();
      } else {
        console.warn('Conversation created but no conversation object returned');
        // Still refresh and close - the conversation should appear in the list
        await fetchConversations(token);
        onClose();
      }
    } catch (error: any) {
      console.error('Failed to create conversation - Full error:', error);
      // Only show error if it's a real error, not just a missing conversation object
      if (error.message && !error.message.includes('Loading')) {
        const errorMessage = error.response?.data?.detail || error.message || 'Unknown error';
        alert(`Failed to create conversation: ${errorMessage}. Check console for details.`);
      } else {
        // Conversation might have been created, just refresh
        await fetchConversations(token);
        onClose();
      }
    } finally {
      setIsCreating(false);
    }
  };

  const filteredEmployees = employees.filter((emp) =>
    emp.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    emp.email.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md mx-4">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-gray-900">New Conversation</h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700 text-2xl font-bold w-8 h-8 flex items-center justify-center rounded hover:bg-gray-100"
            aria-label="Close"
          >
            ×
          </button>
        </div>

        <div className="mb-4">
          <Input
            placeholder="Search by name or email..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full"
          />
        </div>

        <div className="max-h-96 overflow-y-auto">
          {isLoading ? (
            <div className="text-center text-gray-500 py-8">Loading employees...</div>
          ) : filteredEmployees.length === 0 ? (
            <div className="text-center text-gray-500 py-8">
              {searchQuery ? 'No employees found' : 'No employees available'}
            </div>
          ) : (
            <div className="space-y-2">
              {filteredEmployees.map((emp) => {
                const empId = emp.id || emp._id;
                return (
                <button
                  key={empId}
                  onClick={() => handleCreateConversation(empId!)}
                  disabled={isCreating}
                  className="w-full px-4 py-3 rounded-lg hover:bg-gray-50 border border-gray-200 text-left transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-brand-600 text-white flex items-center justify-center font-semibold flex-shrink-0">
                      {emp.picture ? (
                        <img src={emp.picture} alt={emp.name} className="w-full h-full rounded-full object-cover" />
                      ) : (
                        emp.name[0]
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-gray-900 truncate">{emp.name}</p>
                      <p className="text-sm text-gray-500 truncate">{emp.email}</p>
                    </div>
                  </div>
                </button>
                );
              })}
            </div>
          )}
        </div>

        <div className="mt-4 flex justify-end">
          <Button onClick={onClose} variant="secondary">
            Cancel
          </Button>
        </div>
      </div>
    </div>
  );
}

