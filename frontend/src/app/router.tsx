import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AssistantPage } from '../features/assistant/pages/AssistantPage';
import { CreateTicketPage } from '../features/tickets/pages/CreateTicketPage';
import { TicketDetailPage } from '../features/tickets/pages/TicketDetailPage';
import { TicketListPage } from '../features/tickets/pages/TicketListPage';
import { AppLayout } from './App';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/tickets" replace /> },
      { path: 'tickets', element: <TicketListPage /> },
      { path: 'tickets/new', element: <CreateTicketPage /> },
      { path: 'tickets/:id', element: <TicketDetailPage /> },
      { path: 'assistant', element: <AssistantPage /> },
    ],
  },
]);
