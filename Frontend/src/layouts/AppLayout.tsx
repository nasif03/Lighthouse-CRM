import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { TenantSwitcher } from '../components/TenantSwitcher';
import { clsx } from 'clsx';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { IconAnalytics, IconCampaigns, IconContacts, IconDashboard, IconLeads, IconSegments, IconSettings, IconTemplates } from '../components/icons';
import InboxPanel from '../components/inbox/InboxPanel';

const navItems = [
  { to: '/', label: 'Dashboard', icon: IconDashboard },
  { to: '/leads', label: 'Leads', icon: IconLeads },
  { to: '/contacts', label: 'Contacts', icon: IconContacts },
  { to: '/campaigns', label: 'Campaigns', icon: IconCampaigns },
  { to: '/segments', label: 'Segments', icon: IconSegments },
  { to: '/templates', label: 'Templates', icon: IconTemplates },
  { to: '/analytics', label: 'Analytics', icon: IconAnalytics },
  { to: '/settings', label: 'Settings', icon: IconSettings },
];

export default function AppLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const { pathname } = useLocation();
  const title = navItems.find(n => n.to === pathname)?.label || 'Overview';

  return (
    <div className="h-full grid grid-cols-[260px_1fr_420px] grid-rows-[56px_1fr]">
      <aside className="row-span-2 bg-gray-50 border-r border-gray-200 p-3">
        <div className="flex items-center gap-2 px-2 py-3">
          <div className="size-8 rounded bg-brand-600" />
          <div className="text-lg font-semibold">Lighthouse CRM</div>
        </div>
        <nav className="mt-4 flex flex-col gap-1">
          {navItems.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.to === '/'}
              className={({ isActive }) => clsx(
                'px-3 py-2 rounded-md text-sm flex items-center gap-2',
                isActive ? 'bg-brand-50 text-brand-700 border-l-2 border-brand-600' : 'text-gray-700 hover:bg-gray-100'
              )}
            >
              <n.icon className="w-4 h-4" />
              <span>{n.label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>

      <header className="col-start-2 h-14 border-b border-gray-200 flex items-center gap-3 px-4">
        <div className="text-lg font-semibold tracking-tight">{title}</div>
        <div className="mx-3 h-6 w-px bg-gray-200" />
        <div className="hidden md:flex items-center gap-2">
          <Input placeholder="Search..." className="w-64" />
        </div>
        <div className="mx-3 h-6 w-px bg-gray-200" />
        <TenantSwitcher />
        <div className="ml-auto flex items-center gap-3">
          {user ? (
            <>
              <span className="text-sm text-gray-600">{user.name}</span>
              <button
                className="px-3 py-1.5 rounded bg-gray-100 hover:bg-gray-200 text-sm"
                onClick={() => { logout(); navigate('/login'); }}
              >
                Logout
              </button>
            </>
          ) : (
            <Button onClick={() => navigate('/login')}>Login</Button>
          )}
          <Button className="hidden md:inline-flex">New</Button>
        </div>
      </header>

      <main className="col-start-2 overflow-auto p-4 bg-white">
        <Outlet />
      </main>

      {/* Inbox Panel on the right */}
      <div className="col-start-3 row-span-2 h-full relative">
        <InboxPanel />
      </div>
    </div>
  );
}


