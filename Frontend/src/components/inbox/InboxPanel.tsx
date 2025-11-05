import { useInboxStore } from '../../store/inboxStore';
import InboxSidebar from './InboxSidebar';
import ConversationView from './ConversationView';

export default function InboxPanel() {
	const { activeConversationId } = useInboxStore();

	// Show conversation view if active, otherwise show inbox sidebar
	// Both use full width of the panel (420px) with matching background
	if (activeConversationId) {
		return (
			<div className="h-full w-full">
				<ConversationView />
			</div>
		);
	}

	return (
		<div className="h-full w-full">
			<InboxSidebar />
		</div>
	);
}

