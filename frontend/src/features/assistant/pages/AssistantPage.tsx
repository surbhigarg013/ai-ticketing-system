import { useCallback, useState } from 'react';
import { ApiError } from '../../../shared/api/client';
import { ErrorBanner } from '../../../shared/components/ErrorBanner';
import { LoadingSpinner } from '../../../shared/components/LoadingSpinner';
import { askQuestion, type AskResponse } from '../api/assistantApi';
import { AnswerPanel } from '../components/AnswerPanel';
import { QuestionForm } from '../components/QuestionForm';

function mapFieldErrors(err: ApiError): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const violation of err.problem.errors ?? []) {
    errors[violation.field] = violation.message;
  }
  return errors;
}

export function AssistantPage() {
  const [response, setResponse] = useState<AskResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const handleAsk = useCallback(async (question: string) => {
    setLoading(true);
    setError('');
    setFieldErrors({});
    try {
      const result = await askQuestion({ question });
      setResponse(result);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 400 && err.problem.errors?.length) {
          setFieldErrors(mapFieldErrors(err));
        } else if (err.status === 503) {
          setError('Assistant is temporarily unavailable. Please try again later.');
        } else {
          setError(err.message);
        }
      } else {
        setError('Failed to get an answer. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  return (
    <section aria-live="polite">
      <header className="page-header">
        <h1>Knowledge Assistant</h1>
      </header>

      <p className="assistant-intro">
        Ask natural-language questions over indexed ticket history. Answers are grounded in ticket
        content with citations when relevant matches are found.
      </p>

      <QuestionForm onSubmit={handleAsk} disabled={loading} fieldErrors={fieldErrors} />

      <ErrorBanner message={error} onDismiss={() => setError('')} />

      {loading && <LoadingSpinner label="Loading answer" />}

      {response && !loading && <AnswerPanel response={response} />}
    </section>
  );
}
