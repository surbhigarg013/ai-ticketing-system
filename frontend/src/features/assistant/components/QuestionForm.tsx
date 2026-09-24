import { FormEvent, useState } from 'react';

interface QuestionFormProps {
  onSubmit: (question: string) => Promise<void>;
  disabled?: boolean;
  fieldErrors?: Record<string, string>;
}

export function QuestionForm({
  onSubmit,
  disabled = false,
  fieldErrors = {},
}: QuestionFormProps) {
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

  const serverQuestionError = fieldErrors.question;
  const activeError = serverQuestionError ?? validationError;

  return (
    <form onSubmit={handleSubmit} className="question-form" aria-busy={disabled || submitting}>
      <label>
        Ask a question about ticket history
        <textarea
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          rows={4}
          maxLength={2000}
          placeholder="e.g. Have we seen payment failures before?"
          required
          aria-invalid={Boolean(activeError)}
          aria-describedby={activeError ? 'question-error' : undefined}
        />
      </label>
      {activeError && (
        <span id="question-error" className="field-error" role="alert">
          {activeError}
        </span>
      )}
      <button type="submit" disabled={disabled || submitting || question.trim().length < 3}>
        {submitting ? 'Asking…' : 'Ask'}
      </button>
    </form>
  );
}
