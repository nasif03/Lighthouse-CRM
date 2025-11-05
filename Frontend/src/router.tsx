import { createBrowserRouter } from 'react-router-dom';
import AppLayout from './layouts/AppLayout';
import Dashboard from './pages/Dashboard';
import Leads from './pages/Leads';
import Contacts from './pages/Contacts';
import Campaigns from './pages/Campaigns';
import Segments from './pages/Segments';
import Templates from './pages/Templates';
import Analytics from './pages/Analytics';
import Settings from './pages/Settings';
import Login from './pages/Login';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'leads', element: <Leads /> },
      { path: 'contacts', element: <Contacts /> },
      { path: 'campaigns', element: <Campaigns /> },
      { path: 'segments', element: <Segments /> },
      { path: 'templates', element: <Templates /> },
      { path: 'analytics', element: <Analytics /> },
      { path: 'settings', element: <Settings /> },
    ],
  },
  {
    path: '/login',
    element: <Login />
  }
]);


