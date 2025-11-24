import { create } from 'zustand';
import { apiGet } from '../utils/api';

export type FirefliesSummary = {
  overview?: string | null;
  short_summary?: string | null;
};

export type FirefliesTranscript = {
  id: string;
  title?: string | null;
  date?: string | null;
  transcript_url?: string | null;
  summary?: FirefliesSummary | null;
};

type FirefliesState = {
  transcripts: FirefliesTranscript[];
  isLoading: boolean;
  isSyncing: boolean;
  error: string | null;
  lastFetchedAt: string | null;
  lastSyncedAt: string | null;
  fetchTranscripts: (token: string | null, limit?: number) => Promise<void>;
  syncTranscripts: (token: string | null, limit?: number) => Promise<number>;
  clearError: () => void;
};

const DEFAULT_LIMIT = 20;

export const useFirefliesStore = create<FirefliesState>((set, get) => ({
  transcripts: [],
  isLoading: false,
  isSyncing: false,
  error: null,
  lastFetchedAt: null,
  lastSyncedAt: null,

  fetchTranscripts: async (token, limit = DEFAULT_LIMIT) => {
    if (!token) {
      set({ error: 'You must be logged in to load transcripts.' });
      return;
    }

    try {
      set({ isLoading: true, error: null });
      const data = await apiGet<FirefliesTranscript[]>(
        `/api/fireflies/transcripts?limit=${limit}`,
        token,
        { skipCache: true }
      );

      set({
        transcripts: data,
        isLoading: false,
        lastFetchedAt: new Date().toISOString(),
      });
    } catch (error: any) {
      console.error('Failed to fetch Fireflies transcripts:', error);
      set({
        error: error.message || 'Unable to load transcripts.',
        isLoading: false,
      });
    }
  },

  syncTranscripts: async (token, limit = DEFAULT_LIMIT) => {
    if (!token) {
      set({ error: 'You must be logged in to sync transcripts.' });
      return 0;
    }

    try {
      set({ isSyncing: true, error: null });

      const response = await apiGet<{ saved_transcripts: number }>(
        `/api/fireflies/sync_transcripts?limit=${limit}`,
        token,
        { skipCache: true }
      );

      const saved = response?.saved_transcripts ?? 0;
      set({ lastSyncedAt: new Date().toISOString(), isSyncing: false });

      // Refresh list with the latest data
      await get().fetchTranscripts(token, limit);

      return saved;
    } catch (error: any) {
      console.error('Failed to sync Fireflies transcripts:', error);
      set({
        error: error.message || 'Unable to sync transcripts.',
        isSyncing: false,
      });
      return 0;
    }
  },

  clearError: () => set({ error: null }),
}));

