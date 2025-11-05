import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { TenantSwitcher } from '../components/TenantSwitcher';
import { clsx } from 'clsx';

const navItems = [
  { to: '/', label: 'Dashboard' },
  { to: '/leads', label: 'Leads' },
  { to: '/contacts', label: 'Contacts' },
  { to: '/campaigns', label: 'Campaigns' },
  { to: '/segments', label: 'Segments' },
  { to: '/templates', label: 'Templates' },
  { to: '/analytics', label: 'Analytics' },
  { to: '/settings', label: 'Settings' },
];

export default function AppLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  return (
    <div className="h-full grid grid-cols-[260px_1fr] grid-rows-[56px_1fr]">
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
                'px-3 py-2 rounded text-sm',
                isActive ? 'bg-brand-50 text-brand-700' : 'text-gray-700 hover:bg-gray-100'
              )}
            >
              {n.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <header className="col-start-2 h-14 border-b border-gray-200 flex items-center gap-3 px-4">
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
            <button
              className="px-3 py-1.5 rounded bg-brand-600 text-white text-sm"
              onClick={() => navigate('/login')}
            >
              Login
            </button>
          )}
        </div>
      </header>

      <main className="col-start-2 overflow-auto p-4 bg-white">
        <Outlet />
      </main>
    </div>
  );
}


