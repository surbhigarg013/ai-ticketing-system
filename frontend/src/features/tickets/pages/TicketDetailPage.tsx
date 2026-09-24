import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { ApiError } from '../../../shared/api/client';
import { ErrorBanner } from '../../../shared/components/ErrorBanner';
import { LoadingSpinner } from '../../../shared/components/LoadingSpinner';
import {
  addComment,
  getTicket,
  transitionStatus,
  updateTicket,
  type Category,
  type Priority,
  type TicketDetail,
  type TicketStatus,
} from '../api/ticketApi';
import { CommentForm } from '../components/CommentForm';
import { CommentList } from '../components/CommentList';
import { StatusActions } from '../components/StatusActions';

export function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [ticket, setTicket] = useState<TicketDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [category, setCategory] = useState<Category>('GENERAL');
  const [assignee, setAssignee] = useState('');

  const loadTicket = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError('');
    try {
      const detail = await getTicket(id);
      setTicket(detail);
      setTitle(detail.title);
      setDescription(detail.description);
      setPriority(detail.priority);
      setCategory(detail.category);
      setAssignee(detail.assignee ?? '');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load ticket');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadTicket();
  }, [loadTicket]);

  async function handleUpdate() {
    if (!id) return;
    setError('');
    setFieldErrors({});
    try {
      const updated = await updateTicket(id, {
        title,
        description,
        priority,
        category,
        assignee: assignee.trim() || null,
      });
      setTicket(updated);
      setEditing(false);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
        const errors: Record<string, string> = {};
        err.problem.errors?.forEach((e) => {
          errors[e.field] = e.message;
        });
        setFieldErrors(errors);
      } else {
        setError('Failed to update ticket');
      }
    }
  }

  async function handleAddComment(content: string, author: string) {
    if (!id) return;
    setError('');
    await addComment(id, { content, author });
    await loadTicket();
  }

  async function handleTransition(status: TicketStatus, resolution?: string) {
    if (!id) return;
    setError('');
    try {
      const updated = await transitionStatus(id, { status, resolution });
      setTicket(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to transition status');
    }
  }

  if (loading) {
    return <LoadingSpinner />;
  }

  if (!ticket) {
    return <ErrorBanner message={error || 'Ticket not found'} />;
  }

  return (
    <section>
      <header className="page-header">
        <h1>{ticket.displayId}: {ticket.title}</h1>
        <button type="button" onClick={() => setEditing((v) => !v)}>
          {editing ? 'Cancel Edit' : 'Edit'}
        </button>
      </header>

      <ErrorBanner message={error} onDismiss={() => setError('')} />

      {editing ? (
        <div className="edit-form">
          <label>
            Title
            <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} />
            {fieldErrors.title && <span className="field-error">{fieldErrors.title}</span>}
          </label>
          <label>
            Description
            <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={5} />
          </label>
          <label>
            Priority
            <select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
            </select>
          </label>
          <label>
            Category
            <select value={category} onChange={(e) => setCategory(e.target.value as Category)}>
              <option value="PAYMENT">PAYMENT</option>
              <option value="SHIPMENT">SHIPMENT</option>
              <option value="ACCOUNT">ACCOUNT</option>
              <option value="GENERAL">GENERAL</option>
            </select>
          </label>
          <label>
            Assignee
            <input value={assignee} onChange={(e) => setAssignee(e.target.value)} maxLength={100} />
          </label>
          <button type="button" onClick={handleUpdate}>Save Changes</button>
        </div>
      ) : (
        <dl className="ticket-detail">
          <dt>Status</dt><dd>{ticket.status}</dd>
          <dt>Priority</dt><dd>{ticket.priority}</dd>
          <dt>Category</dt><dd>{ticket.category}</dd>
          <dt>Assignee</dt><dd>{ticket.assignee ?? '—'}</dd>
          <dt>Description</dt><dd>{ticket.description}</dd>
          {ticket.resolution && (
            <>
              <dt>Resolution</dt><dd>{ticket.resolution}</dd>
            </>
          )}
        </dl>
      )}

      <section>
        <h2>Status Actions</h2>
        <StatusActions currentStatus={ticket.status} onTransition={handleTransition} />
      </section>

      <section>
        <h2>Comments</h2>
        <CommentList comments={ticket.comments} />
        <CommentForm onSubmit={handleAddComment} />
      </section>
    </section>
  );
}
