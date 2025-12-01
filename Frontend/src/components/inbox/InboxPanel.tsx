import { useInboxStore } from '../../store/inboxStore';
import InboxSidebar from './InboxSidebar';
import StreamConversationView from './StreamConversationView';
import { Chat } from 'stream-chat-react';
import { useStreamChat } from '../../hooks/useStreamChat';
import StreamAudioCallManager from './calls/StreamAudioCallManager';
import StreamAudioCallButton from './calls/StreamAudioCallButton';

export default function InboxPanel() {
	const { activeConversationId, setActiveConversation, conversations } = useInboxStore();
	const { client, isLoading } = useStreamChat();

	console.log('[InboxPanel] activeConversationId:', activeConversationId, 'client:', !!client, 'isLoading:', isLoading);

	const handleBack = () => {
		setActiveConversation(null);
	};

	// Get conversation info for chat header
	const conversation = activeConversationId 
		? conversations.find((c) => 
			c.id === activeConversationId || 
			c.cid === activeConversationId || 
			c.channelId === activeConversationId
		)
		: null;
	const otherMember = conversation?.participantName || 'Unknown';

	// Single container with inbox header and chat box below
	return (
		<>
			<div className="w-full flex flex-col h-full min-h-0 overflow-hidden">
				{/* Inbox Header - Always visible */}
				<div className="px-4 py-3 border-b border-gray-200 flex items-center justify-between flex-shrink-0 bg-white">
					<h2 className="text-lg font-semibold">Inbox</h2>
				</div>

				{activeConversationId ? (
					// Chat Mode: Show chat header + messages
					<>
						{/* Chat Header - Small section showing who you're chatting with + back button */}
						<div className="px-3 py-2 border-b border-gray-200 flex items-center justify-between flex-shrink-0 bg-white">
							<div className="flex items-center gap-2">
								<button
									onClick={handleBack}
									className="w-8 h-8 rounded-md hover:bg-gray-100 flex items-center justify-center text-gray-600 hover:text-gray-900 transition-colors"
									title="Back to inbox"
								>
									<svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
										<path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
									</svg>
								</button>
								<div className="w-8 h-8 rounded-full bg-brand-600 text-white flex items-center justify-center font-semibold text-sm overflow-hidden">
									{conversation?.participantAvatar && conversation.participantAvatar.startsWith('http') ? (
										<img 
											src={conversation.participantAvatar} 
											alt={conversation.participantName}
											className="w-full h-full object-cover"
										/>
									) : (
										<span>{otherMember[0]?.toUpperCase() || '?'}</span>
									)}
								</div>
								<span className="text-sm font-semibold text-gray-900">{otherMember}</span>
							</div>
							{conversation && (
								<StreamAudioCallButton
									participantId={conversation.participantId}
									participantName={conversation.participantName}
									conversationId={conversation.channelId || conversation.id || ''}
								/>
							)}
						</div>

						{/* Messages Area - Scrollable */}
						{client ? (
							<Chat client={client}>
								<StreamConversationView />
							</Chat>
						) : isLoading ? (
							<div className="flex-1 flex items-center justify-center min-h-0">
								<div className="text-gray-500">Loading chat...</div>
							</div>
						) : (
							<div className="flex-1 flex items-center justify-center min-h-0 p-4">
								<div className="text-sm text-yellow-600 bg-yellow-50 border border-yellow-200 rounded p-4">
									Chat client not ready. Please wait a moment and try again.
								</div>
							</div>
						)}
					</>
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

