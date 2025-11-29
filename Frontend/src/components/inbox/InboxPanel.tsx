import { useInboxStore } from '../../store/inboxStore';
import InboxSidebar from './InboxSidebar';
import StreamConversationView from './StreamConversationView';
import { Chat } from 'stream-chat-react';
import { useStreamChat } from '../../hooks/useStreamChat';
import StreamAudioCallManager from './calls/StreamAudioCallManager';

export default function InboxPanel() {
	const { activeConversationId, setActiveConversation } = useInboxStore();
	const { client, isLoading } = useStreamChat();

	console.log('[InboxPanel] activeConversationId:', activeConversationId, 'client:', !!client, 'isLoading:', isLoading);

	const handleBack = () => {
		setActiveConversation(null);
	};

	// Single container with inbox header and chat box below
	return (
		<>
			<div className="w-full flex flex-col">
				{/* Inbox Header - Always visible */}
				<div className="px-4 py-3 border-b border-gray-200 flex items-center justify-between flex-shrink-0 bg-white">
					<div className="flex items-center gap-2">
						{activeConversationId && (
							<button
								onClick={handleBack}
								className="w-8 h-8 rounded-md hover:bg-gray-100 flex items-center justify-center text-gray-600 hover:text-gray-900 transition-colors"
								title="Back to inbox"
							>
								<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
								</svg>
							</button>
						)}
						<h2 className="text-lg font-semibold">Inbox</h2>
					</div>
				</div>

				{/* Chat Box Container - Fixed height */}
				{activeConversationId ? (
					// Chat Mode: Show conversation view in fixed-height box
					client ? (
						<Chat client={client}>
							<StreamConversationView />
						</Chat>
					) : isLoading ? (
						<div className="border border-gray-200 rounded-lg flex items-center justify-center" style={{ height: '400px' }}>
							<div className="text-gray-500">Loading chat...</div>
						</div>
					) : (
						<div className="border border-gray-200 rounded-lg flex items-center justify-center p-4" style={{ height: '400px' }}>
							<div className="text-sm text-yellow-600 bg-yellow-50 border border-yellow-200 rounded p-4">
								Chat client not ready. Please wait a moment and try again.
							</div>
						</div>
					)
				) : (
					// Inbox Mode: Show inbox list
					<div className="flex-1 overflow-y-auto min-h-0">
						<InboxSidebar />
					</div>
				)}
			</div>
			<StreamAudioCallManager />
		</>
	);
}

