import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { TenantSwitcher } from '../components/TenantSwitcher';
import { clsx } from 'clsx';
import Button from '../components/ui/Button';
import { IconAnalytics, IconCampaigns, IconContacts, IconDashboard, IconLeads, IconSegments, IconSettings, IconTemplates, IconDeals, IconSupport, IconAdmin } from '../components/icons';
import InboxPanel from '../components/inbox/InboxPanel';

// Navigation sections for business owner organization
const navSections = [
  {
    label: 'Overview',
    items: [
      { to: '/', label: 'Dashboard', icon: IconDashboard },
    ]
  },
  {
    label: 'Sales',
    items: [
      { to: '/leads', label: 'Leads', icon: IconLeads },
      { to: '/contacts', label: 'Contacts', icon: IconContacts },
      { to: '/deals', label: 'Deals', icon: IconDeals },
    ]
  },
  {
    label: 'Marketing',
    items: [
      { to: '/campaigns', label: 'Campaigns', icon: IconCampaigns },
      { to: '/segments', label: 'Segments', icon: IconSegments },
      { to: '/templates', label: 'Templates', icon: IconTemplates },
      { to: '/analytics', label: 'Analytics', icon: IconAnalytics },
    ]
  },
  {
    label: 'Customer Support',
    items: [
      { to: '/support', label: 'Support Tickets', icon: IconSupport },
    ]
  },
  {
    label: 'Administration',
    items: [
      { to: '/administration', label: 'Administration', icon: IconAdmin },
      { to: '/settings', label: 'Settings', icon: IconSettings },
    ]
  }
];

// Flatten all nav items for finding current page title
const allNavItems = navSections.flatMap(section => section.items);

export default function AppLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const { pathname } = useLocation();
  const title = allNavItems.find(n => n.to === pathname)?.label || 'Overview';

  return (
    <div className="h-full grid grid-cols-[280px_1fr_420px] grid-rows-[56px_1fr] bg-gray-50">
      <aside className="row-span-2 bg-white border-r border-gray-200 p-3">
        <div className="flex items-center gap-2 px-2 py-3">
          <img src="/lighthouse_logo.png" alt="Lighthouse CRM" className="size-8 object-contain" />
          <div className="text-lg font-semibold">Lighthouse CRM</div>
        </div>
        <nav className="mt-4 flex flex-col gap-6">
          {navSections.map((section) => (
            <div key={section.label}>
              <div className="px-2 py-1.5 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                {section.label}
              </div>
              <div className="flex flex-col gap-1 mt-1">
                {section.items.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.to === '/'}
                    className={({ isActive }) => clsx(
                      'px-3 py-2 rounded-md text-sm flex items-center gap-2',
                      isActive ? 'bg-brand-50 text-brand-700 border-l-2 border-brand-600' : 'text-gray-700 hover:bg-gray-50'
                    )}
                  >
                    <item.icon className="w-4 h-4" />
                    <span>{item.label}</span>
                  </NavLink>
                ))}
              </div>
            </div>
          ))}
        </nav>
      </aside>

      <header className="col-start-2 col-span-2 h-14 border-b border-gray-200 bg-white flex items-center gap-3 px-4">
        <div className="text-lg font-semibold tracking-tight w-48 min-w-48 truncate">{title}</div>
        <div className="mx-3 h-6 w-px bg-gray-200" />
        <TenantSwitcher />
        <div className="ml-auto flex items-center gap-4">
          {user ? (
            <>
              <span className="text-sm text-gray-600">{user.name}</span>
              <button
                className="px-3 py-1.5 rounded bg-gray-100 hover:bg-gray-200 text-sm"
                onClick={async () => { 
                  await logout(); 
                  navigate('/login'); 
                }}
              >
                Logout
              </button>
            </>
          ) : (
            <Button onClick={() => navigate('/login')}>Login</Button>
          )}
        </div>
      </header>

      <main className="col-start-2 overflow-auto p-4 bg-white">
        <Outlet />
      </main>

      {/* Inbox Panel on the right - integrated into main background */}
      <div className="col-start-3 row-span-2 h-full bg-white border-l border-gray-200">
        <InboxPanel />
      </div>
    </div>
  );
}


