import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
// import { TenantSwitcher } from '../components/TenantSwitcher';
import { clsx } from 'clsx';
import Button from '../components/ui/Button';
import { IconAnalytics, IconCampaigns, IconContacts, IconDashboard, IconLeads, IconSegments, IconSettings, IconTemplates, IconDeals, IconSupport, IconAdmin } from '../components/icons';
import InboxPanel from '../components/inbox/InboxPanel';
import GmailPanel from '../components/gmail/GmailPanel';

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
  // {
  //   label: 'Marketing',
  //   items: [
  //     { to: '/campaigns', label: 'Campaigns', icon: IconCampaigns },
  //     { to: '/segments', label: 'Segments', icon: IconSegments },
  //     { to: '/templates', label: 'Templates', icon: IconTemplates },
  //     { to: '/analytics', label: 'Analytics', icon: IconAnalytics },
  //   ]
  // },
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
    <div className="h-full grid grid-cols-[250px_1fr_350px] grid-rows-[57px_1fr] bg-gray-50">
      <aside className="row-span-2 bg-gradient-to-b from-white to-gray-50 border-r border-gray-200 shadow-sm">
        <div className="flex items-center gap-2 px-3 h-[56px] border-b border-gray-200">
          <img src="/lighthouse_logo.png" alt="Lighthouse CRM" className="size-7 object-contain" />
          <div className="text-base font-bold text-gray-900">Lighthouse</div>
        </div>
        <nav className="p-2 flex flex-col gap-4 mt-2">
          {navSections.map((section) => (
            <div key={section.label}>
              <div className="px-3 py-1.5 text-[10px] font-bold text-gray-400 uppercase tracking-widest">
                {section.label}
              </div>
              <div className="flex flex-col gap-0.5 mt-1">
                {section.items.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.to === '/'}
                    className={({ isActive }) => clsx(
                      'px-3 py-2.5 rounded-lg text-sm flex items-center gap-2.5 transition-all duration-200',
                      isActive 
                        ? 'bg-brand-500 text-white shadow-md shadow-brand-500/20 font-medium' 
                        : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                    )}
                  >
                    <item.icon className="w-4 h-4 flex-shrink-0 transition-colors [&>path]:fill-current" />
                    <span>{item.label}</span>
                  </NavLink>
                ))}
              </div>
            </div>
          ))}
        </nav>
      </aside>

      <header className="col-start-2 col-span-2 h-[56px] border-b border-gray-200 bg-white flex items-center gap-3 px-4">
        <div className="text-lg font-semibold tracking-tight w-48 min-w-48 truncate">{title}</div>
        {/* <div className="mx-3 h-6 w-px bg-gray-200" /> */}
        {/* <TenantSwitcher /> */}
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

      {/* Inbox Panel and Gmail Panel on the right - integrated into main background */}
      <div className="col-start-3 row-span-2 h-full bg-white border-l border-gray-200 flex flex-col">
        <div className="flex-1 border-b border-gray-200">
          <InboxPanel />
        </div>
        <div className="flex-1">
          <GmailPanel />
        </div>
      </div>
    </div>
  );
}


