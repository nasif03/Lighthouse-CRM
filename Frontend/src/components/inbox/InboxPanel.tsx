import { useInboxStore } from '../../store/inboxStore';
import InboxSidebar from './InboxSidebar';
import StreamConversationView from './StreamConversationView';
import { Chat } from 'stream-chat-react';
import { useStreamChat } from '../../hooks/useStreamChat';
import StreamAudioCallManager from './calls/StreamAudioCallManager';

export default function InboxPanel() {
	const { activeConversationId } = useInboxStore();
	const { client, isLoading } = useStreamChat();

	console.log('[InboxPanel] activeConversationId:', activeConversationId, 'client:', !!client, 'isLoading:', isLoading);

	let content = (
		<div className="h-full w-full">
			<InboxSidebar />
		</div>
	);

	if (activeConversationId) {
		if (client) {
			content = (
				<Chat client={client}>
					<div className="h-full w-full">
						<StreamConversationView />
					</div>
				</Chat>
			);
		} else if (isLoading) {
			content = (
				<div className="h-full w-full flex items-center justify-center">
					<div className="text-gray-500">Loading chat...</div>
				</div>
			);
		} else {
			content = (
				<div className="h-full w-full">
					<InboxSidebar />
					<div className="p-4 text-sm text-yellow-600 bg-yellow-50 border border-yellow-200 rounded">
						Chat client not ready. Please wait a moment and try again.
					</div>
				</div>
			);
		}
	}

	return (
		<>
			{content}
			<StreamAudioCallManager />
		</>
	);
}

