import { FormEvent, useState } from 'react';
import type { Category, CreateTicketRequest, Priority } from '../api/ticketApi';

interface TicketFormProps {
  initial?: Partial<CreateTicketRequest>;
  submitLabel: string;
  onSubmit: (request: CreateTicketRequest) => Promise<void>;
  fieldErrors?: Record<string, string>;
}

const PRIORITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH'];
const CATEGORIES: Category[] = ['PAYMENT', 'SHIPMENT', 'ACCOUNT', 'GENERAL'];

export function TicketForm({
  initial,
  submitLabel,
  onSubmit,
  fieldErrors = {},
}: TicketFormProps) {
  const [title, setTitle] = useState(initial?.title ?? '');
  const [description, setDescription] = useState(initial?.description ?? '');
  const [priority, setPriority] = useState<Priority>(initial?.priority ?? 'MEDIUM');
  const [category, setCategory] = useState<Category>(initial?.category ?? 'GENERAL');
  const [assignee, setAssignee] = useState(initial?.assignee ?? '');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!title.trim() || !description.trim()) {
      return;
    }
    setSubmitting(true);
    try {
      await onSubmit({
        title: title.trim(),
        description: description.trim(),
        priority,
        category,
        assignee: assignee.trim() || undefined,
      });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="ticket-form">
      <label>
        Title
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
          maxLength={200}
        />
        {fieldErrors.title && <span className="field-error">{fieldErrors.title}</span>}
      </label>
      <label>
        Description
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          required
          rows={5}
        />
        {fieldErrors.description && (
          <span className="field-error">{fieldErrors.description}</span>
        )}
      </label>
      <label>
        Priority
        <select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
          {PRIORITIES.map((p) => (
            <option key={p} value={p}>{p}</option>
          ))}
        </select>
      </label>
      <label>
        Category
        <select value={category} onChange={(e) => setCategory(e.target.value as Category)}>
          {CATEGORIES.map((c) => (
            <option key={c} value={c}>{c}</option>
          ))}
        </select>
      </label>
      <label>
        Assignee (optional)
        <input
          value={assignee}
          onChange={(e) => setAssignee(e.target.value)}
          maxLength={100}
        />
      </label>
      <button type="submit" disabled={submitting || !title.trim() || !description.trim()}>
        {submitLabel}
      </button>
    </form>
  );
}
