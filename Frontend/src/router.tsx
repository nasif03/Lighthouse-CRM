import { createBrowserRouter, Navigate } from 'react-router-dom';
import AppLayout from './layouts/AppLayout';
import Dashboard from './pages/Dashboard';
import Leads from './pages/Leads';
import Contacts from './pages/Contacts';
import Deals from './pages/Deals';
import Campaigns from './pages/Campaigns';
import Segments from './pages/Segments';
import Templates from './pages/Templates';
import Analytics from './pages/Analytics';
import Support from './pages/Support';
import Administration from './pages/Administration';
import Settings from './pages/Settings';
import Login from './pages/Login';
import SubmitTicket from './pages/SubmitTicket';
import ProtectedRoute from './components/ProtectedRoute';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <Dashboard /> },
          { path: 'leads', element: <Leads /> },
          { path: 'contacts', element: <Contacts /> },
          { path: 'deals', element: <Deals /> },
          { path: 'campaigns', element: <Campaigns /> },
          { path: 'segments', element: <Segments /> },
          { path: 'templates', element: <Templates /> },
          { path: 'analytics', element: <Analytics /> },
          { path: 'support', element: <Support /> },
          { path: 'administration', element: <Administration /> },
          { path: 'settings', element: <Settings /> },
        ],
      },
    ],
  },
  {
    path: '/login',
    element: <Login />
  },
  {
    path: '/ticket/:orgId',
    element: <SubmitTicket />
  },
  {
    path: '*',
    element: <Navigate to="/" replace />
  }
]);


