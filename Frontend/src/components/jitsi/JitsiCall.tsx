import { useEffect, useRef } from 'react';

type Props = {
  roomName: string;
  userName: string;
  onEndCall: () => void;
};

export default function JitsiCall({ roomName, userName, onEndCall }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const apiRef = useRef<any>(null);

  useEffect(() => {
    // Load Jitsi External API script
    const script = document.createElement('script');
    script.src = 'https://meet.jit.si/external_api.js';
    script.async = true;
    
    script.onload = () => {
      if (containerRef.current && (window as any).JitsiMeetExternalAPI) {
        const JitsiMeetExternalAPI = (window as any).JitsiMeetExternalAPI;
        
        // Initialize Jitsi
        apiRef.current = new JitsiMeetExternalAPI('meet.jit.si', {
          roomName: roomName,
          parentNode: containerRef.current,
          width: '100%',
          height: 500,
          userInfo: {
            displayName: userName,
          },
          configOverwrite: {
            startWithAudioMuted: false,
            startWithVideoMuted: true, // Audio-only mode
            toolbarButtons: ['microphone', 'hangup', 'settings'],
          },
          interfaceConfigOverwrite: {
            TOOLBAR_BUTTONS: ['microphone', 'hangup', 'settings'],
            SETTINGS_SECTIONS: ['devices', 'language'],
            HIDE_INVITE_MORE_HEADER: true,
          },
        });

        // Listen for call end
        apiRef.current.addEventListeners({
          readyToClose: () => {
            onEndCall();
          },
          videoConferenceLeft: () => {
            onEndCall();
          },
        });
      }
    };
    
    script.onerror = () => {
      console.error('Failed to load Jitsi External API');
      alert('Failed to load call interface. Please check your internet connection.');
    };
    
    document.body.appendChild(script);

    // Cleanup
    return () => {
      if (apiRef.current) {
        try {
          apiRef.current.dispose();
        } catch (e) {
          console.error('Error disposing Jitsi:', e);
        }
      }
      if (script.parentNode) {
        script.parentNode.removeChild(script);
      }
    };
  }, [roomName, userName, onEndCall]);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-4 w-full max-w-3xl mx-4">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold text-gray-900">Audio Call</h3>
          <button
            onClick={onEndCall}
            className="text-gray-500 hover:text-gray-700 text-xl font-bold w-8 h-8 flex items-center justify-center rounded hover:bg-gray-100"
            aria-label="Close call"
          >
            ×
          </button>
        </div>
        <div 
          ref={containerRef} 
          className="w-full rounded overflow-hidden"
          style={{ height: '500px', minHeight: '400px' }}
        />
      </div>
    </div>
  );
}

