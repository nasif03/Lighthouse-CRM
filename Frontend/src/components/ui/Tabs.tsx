import { ReactNode, useState } from 'react';

type Tab = { id: string; label: string; content: ReactNode };

export default function Tabs({ tabs, defaultTabId }: { tabs: Tab[]; defaultTabId?: string }) {
  const [active, setActive] = useState<string>(defaultTabId ?? tabs[0]?.id);
  const current = tabs.find(t => t.id === active) ?? tabs[0];
  return (
    <div>
      <div className="flex border-b border-gray-200">
        {tabs.map(t => (
          <button
            key={t.id}
            className={`px-3 py-2 text-sm -mb-px border-b-2 ${t.id === active ? 'border-brand-600 text-brand-700' : 'border-transparent text-gray-600'}`}
            onClick={() => setActive(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>
      <div className="pt-3">
        {current?.content}
      </div>
    </div>
  );
}


