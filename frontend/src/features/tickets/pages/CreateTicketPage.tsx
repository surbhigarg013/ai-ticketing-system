import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '../../../shared/api/client';
import { ErrorBanner } from '../../../shared/components/ErrorBanner';
import { createTicket } from '../api/ticketApi';
import { TicketForm } from '../components/TicketForm';

export function CreateTicketPage() {
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  return (
    <section>
      <h1>Create Ticket</h1>
      <ErrorBanner message={error} onDismiss={() => setError('')} />
      <TicketForm
        submitLabel="Create Ticket"
        fieldErrors={fieldErrors}
        onSubmit={async (request) => {
          setError('');
          setFieldErrors({});
          try {
            const created = await createTicket(request);
            navigate(`/tickets/${created.id}`);
          } catch (err) {
            if (err instanceof ApiError) {
              setError(err.message);
              const errors: Record<string, string> = {};
              err.problem.errors?.forEach((e) => {
                errors[e.field] = e.message;
              });
              setFieldErrors(errors);
            } else {
              setError('Failed to create ticket');
            }
          }
        }}
      />
    </section>
  );
}
