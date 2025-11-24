import { useInboxStore } from '../../store/inboxStore';
import InboxSidebar from './InboxSidebar';
import StreamConversationView from './StreamConversationView';
import { Chat } from 'stream-chat-react';
import { useStreamChat } from '../../hooks/useStreamChat';

export default function InboxPanel() {
	const { activeConversationId } = useInboxStore();
	const { client, isLoading } = useStreamChat();

	console.log('[InboxPanel] activeConversationId:', activeConversationId, 'client:', !!client, 'isLoading:', isLoading);

	// Show conversation view if active, otherwise show inbox sidebar
	if (activeConversationId) {
		// Wrap in Stream Chat provider if client is ready
		if (client) {
			return (
				<Chat client={client}>
					<div className="h-full w-full">
						<StreamConversationView />
					</div>
				</Chat>
			);
		}
		
		// Show loading only if we're actually loading, otherwise show sidebar
		if (isLoading) {
			return (
				<div className="h-full w-full flex items-center justify-center">
					<div className="text-gray-500">Loading chat...</div>
				</div>
			);
		}
		
		// If client isn't ready but we have an active conversation, show sidebar with message
		return (
			<div className="h-full w-full">
				<InboxSidebar />
				<div className="p-4 text-sm text-yellow-600 bg-yellow-50 border border-yellow-200 rounded">
					Chat client not ready. Please wait a moment and try again.
				</div>
			</div>
		);
	}

	return (
		<div className="h-full w-full">
			<InboxSidebar />
		</div>
	);
}

