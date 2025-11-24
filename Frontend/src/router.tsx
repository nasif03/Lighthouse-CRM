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
import Fireflies from './pages/Fireflies';
import Support from './pages/Support';
import SupportAI from './pages/SupportAI';
import CreateTicket from './pages/CreateTicket';
import TicketDetail from './pages/TicketDetail';
import Administration from './pages/Administration';
import Settings from './pages/Settings';
import Login from './pages/Login';
import SubmitTicket from './pages/SubmitTicket';
import ProtectedRoute from './components/ProtectedRoute';
import ErrorPage from './components/ErrorPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <ProtectedRoute />,
    errorElement: <ErrorPage />,
    children: [
      {
        element: <AppLayout />,
        errorElement: <ErrorPage />,
        children: [
          { index: true, element: <Dashboard /> },
          { path: 'leads', element: <Leads /> },
          { path: 'contacts', element: <Contacts /> },
          { path: 'deals', element: <Deals /> },
          { path: 'campaigns', element: <Campaigns /> },
          { path: 'segments', element: <Segments /> },
          { path: 'templates', element: <Templates /> },
          { path: 'analytics', element: <Analytics /> },
          { path: 'fireflies', element: <Fireflies /> },
          { path: 'support-ai', element: <SupportAI /> },
          { path: 'support', element: <Support /> },
          { path: 'support/create', element: <CreateTicket /> },
          { path: 'support/:id', element: <TicketDetail /> },
          { path: 'administration', element: <Administration /> },
          { path: 'settings', element: <Settings /> },
        ],
      },
    ],
  },
  {
    path: '/login',
    element: <Login />,
    errorElement: <ErrorPage />
  },
  {
    path: '/ticket/:orgId',
    element: <SubmitTicket />,
    errorElement: <ErrorPage />
  },
  {
    path: '*',
    element: <Navigate to="/" replace />
  }
]);


