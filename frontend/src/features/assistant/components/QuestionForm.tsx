import { FormEvent, useState } from 'react';

interface QuestionFormProps {
  onSubmit: (question: string) => Promise<void>;
  disabled?: boolean;
}

export function QuestionForm({ onSubmit, disabled = false }: QuestionFormProps) {
  const [question, setQuestion] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [validationError, setValidationError] = useState('');

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmed = question.trim();
    if (trimmed.length < 3) {
      setValidationError('Question must be at least 3 characters.');
      return;
    }
    if (trimmed.length > 2000) {
      setValidationError('Question must be at most 2000 characters.');
      return;
    }

    setValidationError('');
    setSubmitting(true);
    try {
      await onSubmit(trimmed);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="question-form">
      <label>
        Ask a question about ticket history
        <textarea
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          rows={4}
          maxLength={2000}
          placeholder="e.g. Have we seen payment failures before?"
          required
        />
      </label>
      {validationError && <span className="field-error">{validationError}</span>}
      <button type="submit" disabled={disabled || submitting || question.trim().length < 3}>
        {submitting ? 'Asking…' : 'Ask'}
      </button>
    </form>
  );
}
