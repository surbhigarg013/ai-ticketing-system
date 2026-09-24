import { FormEvent, useState } from 'react';
import {
  VALID_TRANSITIONS,
  type TicketStatus,
} from '../api/ticketApi';

interface StatusActionsProps {
  currentStatus: TicketStatus;
  onTransition: (status: TicketStatus, resolution?: string) => Promise<void>;
}

export function StatusActions({ currentStatus, onTransition }: StatusActionsProps) {
  const nextStates = VALID_TRANSITIONS[currentStatus];
  const [selected, setSelected] = useState<TicketStatus | ''>('');
  const [resolution, setResolution] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (nextStates.length === 0) {
    return <p className="status-terminal">No further transitions available.</p>;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!selected) return;
    setSubmitting(true);
    try {
      await onTransition(
        selected,
        selected === 'RESOLVED' ? resolution.trim() : undefined,
      );
      setSelected('');
      setResolution('');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="status-actions">
      <label>
        Transition to
        <select
          value={selected}
          onChange={(e) => setSelected(e.target.value as TicketStatus)}
          required
        >
          <option value="">Select status…</option>
          {nextStates.map((status) => (
            <option key={status} value={status}>
              {status.replace('_', ' ')}
            </option>
          ))}
        </select>
      </label>
      {selected === 'RESOLVED' && (
        <label>
          Resolution (required)
          <textarea
            value={resolution}
            onChange={(e) => setResolution(e.target.value)}
            required
            rows={4}
          />
        </label>
      )}
      <button
        type="submit"
        disabled={
          submitting ||
          !selected ||
          (selected === 'RESOLVED' && !resolution.trim())
        }
      >
        Apply Transition
      </button>
    </form>
  );
}
