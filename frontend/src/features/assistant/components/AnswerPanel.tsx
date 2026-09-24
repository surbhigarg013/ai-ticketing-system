import type { AskResponse } from '../api/assistantApi';
import { SourceList } from './SourceList';

interface AnswerPanelProps {
  response: AskResponse;
}

export function AnswerPanel({ response }: AnswerPanelProps) {
  return (
    <section className="answer-panel">
      <h2>Answer</h2>
      <p className="answer-text">{response.answer}</p>
      {response.sources.length > 0 && <SourceList sources={response.sources} />}
    </section>
  );
}
