import { useCallback, useState } from 'react';
import { ApiError } from '../../../shared/api/client';
import { ErrorBanner } from '../../../shared/components/ErrorBanner';
import { LoadingSpinner } from '../../../shared/components/LoadingSpinner';
import { askQuestion, type AskResponse } from '../api/assistantApi';
import { AnswerPanel } from '../components/AnswerPanel';
import { QuestionForm } from '../components/QuestionForm';

export function AssistantPage() {
  const [response, setResponse] = useState<AskResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleAsk = useCallback(async (question: string) => {
    setLoading(true);
    setError('');
    try {
      const result = await askQuestion({ question });
      setResponse(result);
    } catch (err) {
      setResponse(null);
      if (err instanceof ApiError) {
        if (err.status === 503) {
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
    <section>
      <header className="page-header">
        <h1>Knowledge Assistant</h1>
      </header>

      <p className="assistant-intro">
        Ask natural-language questions over indexed ticket history. Answers are grounded in ticket
        content with citations when relevant matches are found.
      </p>

      <QuestionForm onSubmit={handleAsk} disabled={loading} />

      <ErrorBanner message={error} onDismiss={() => setError('')} />

      {loading && <LoadingSpinner />}

      {response && !loading && <AnswerPanel response={response} />}
    </section>
  );
}
