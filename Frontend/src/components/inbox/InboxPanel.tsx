import { useInboxStore } from '../../store/inboxStore';
import InboxSidebar from './InboxSidebar';
import ConversationView from './ConversationView';

export default function InboxPanel() {
	const { activeConversationId } = useInboxStore();

	// Show conversation view if active, otherwise show inbox sidebar
	// Both use full width of the panel (420px)
	if (activeConversationId) {
		return (
			<div className="h-full w-full border-l border-gray-200 bg-white">
				<ConversationView />
			</div>
		);
	}

	return (
		<div className="h-full w-full border-l border-gray-200 bg-white">
			<InboxSidebar />
		</div>
	);
}

