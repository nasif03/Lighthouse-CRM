import { create } from 'zustand';
import { apiGet, apiPost } from '../utils/api';

type CallStatus = {
  sid: string;
  status: string;
  to: string;
  from: string;
  duration?: string;
  start_time?: string;
  end_time?: string;
};

type TwilioState = {
  isLoading: boolean;
  error: string | null;
  makeCall: (token: string, to: string, message?: string) => Promise<CallStatus>;
  getCallStatus: (token: string, callSid: string) => Promise<CallStatus>;
  getAllowedNumber: (token: string) => Promise<string>;
  clearError: () => void;
};

export const useTwilioStore = create<TwilioState>((set) => ({
  isLoading: false,
  error: null,

  makeCall: async (token: string, to: string, message?: string) => {
    try {
      set({ isLoading: true, error: null });
      const response = await apiPost<{
        success: boolean;
        call_sid: string;
        status: string;
        to: string;
        from: string;
        message?: string;
      }>(
        '/api/twilio/call',
        token,
        { to, message: message || 'Hello, this is a call from Lighthouse CRM.' },
        { skipCache: true }
      );

      set({ isLoading: false });
      return {
        sid: response.call_sid,
        status: response.status,
        to: response.to,
        from: response.from,
      };
    } catch (error: any) {
      console.error('Error making call:', error);
      set({
        error: error.message || 'Failed to make call',
        isLoading: false,
      });
      throw error;
    }
  },

  getCallStatus: async (token: string, callSid: string) => {
    try {
      set({ isLoading: true, error: null });
      const response = await apiGet<CallStatus>(
        `/api/twilio/call/${callSid}`,
        token
      );

      set({ isLoading: false });
      return response;
    } catch (error: any) {
      console.error('Error getting call status:', error);
      set({
        error: error.message || 'Failed to get call status',
        isLoading: false,
      });
      throw error;
    }
  },

  getAllowedNumber: async (token: string) => {
    try {
      const response = await apiGet<{ allowed_number: string; message: string }>(
        '/api/twilio/allowed-number',
        token
      );
      return response.allowed_number;
    } catch (error: any) {
      console.error('Error getting allowed number:', error);
      throw error;
    }
  },

  clearError: () => {
    set({ error: null });
  },
}));

