import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../../../shared/api/client';
import { ErrorBanner } from '../../../shared/components/ErrorBanner';
import { LoadingSpinner } from '../../../shared/components/LoadingSpinner';
import {
  listTickets,
  type TicketPage,
  type TicketStatus,
} from '../api/ticketApi';
import { SearchBar } from '../components/SearchBar';
import { StatusFilter } from '../components/StatusFilter';

export function TicketListPage() {
  const [data, setData] = useState<TicketPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<TicketStatus | ''>('');

  const loadTickets = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const page = await listTickets({
        q: keyword || undefined,
        status: status || undefined,
      });
      setData(page);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load tickets');
    } finally {
      setLoading(false);
    }
  }, [keyword, status]);

  useEffect(() => {
    loadTickets();
  }, [loadTickets]);

  return (
    <section>
      <header className="page-header">
        <h1>Tickets</h1>
        <Link to="/tickets/new" className="button-link">New Ticket</Link>
      </header>

      <div className="list-filters">
        <SearchBar value={keyword} onChange={setKeyword} />
        <StatusFilter value={status} onChange={setStatus} />
      </div>

      <ErrorBanner message={error} onDismiss={() => setError('')} />

      {loading ? (
        <LoadingSpinner />
      ) : data && data.items.length === 0 ? (
        <p className="empty-state">No tickets found.</p>
      ) : (
        <table className="ticket-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Title</th>
              <th>Status</th>
              <th>Priority</th>
            </tr>
          </thead>
          <tbody>
            {data?.items.map((ticket) => (
              <tr key={ticket.id}>
                <td>
                  <Link to={`/tickets/${ticket.id}`}>{ticket.displayId}</Link>
                </td>
                <td>{ticket.title}</td>
                <td>{ticket.status}</td>
                <td>{ticket.priority}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
