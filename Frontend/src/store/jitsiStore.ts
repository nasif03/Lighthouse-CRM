import { create } from 'zustand';

type JitsiState = {
  isCallActive: boolean;
  currentRoom: string | null;
  participants: { userId1: string; userId2: string } | null;
  startCall: (userId1: string, userId2: string) => string;
  endCall: () => void;
};

export const useJitsiStore = create<JitsiState>((set) => ({
  isCallActive: false,
  currentRoom: null,
  participants: null,
  
  startCall: (userId1: string, userId2: string) => {
    // Generate room name for CRM-to-CRM calls
    // For testing: Since conversations are mock data with participantId like 'alex', 'sarah', etc.
    // Both users calling the same conversation should join the same room
    // We use the participantId (userId2) as the room identifier for testing
    // In production with real user IDs, we'd use: crm-{sortedUserIds}
    const roomName = `crm-${userId2}`;
    
    set({ 
      isCallActive: true, 
      currentRoom: roomName,
      participants: { userId1, userId2 }
    });
    
    return roomName;
  },
  
  endCall: () => {
    set({ 
      isCallActive: false, 
      currentRoom: null,
      participants: null
    });
  },
}));

