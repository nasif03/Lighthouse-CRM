import { create } from 'zustand';

type User = { id: string; name: string; email: string } | null;

type AuthState = {
  user: User;
  login: (user: NonNullable<User>) => void;
  logout: () => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  user: { id: '1', name: 'Alex Admin', email: 'alex@example.com' },
  login: (user) => set({ user }),
  logout: () => set({ user: null }),
}));


