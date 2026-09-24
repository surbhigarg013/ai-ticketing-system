import { Link } from 'react-router-dom';
import type { Source } from '../api/assistantApi';

interface SourceListProps {
  sources: Source[];
}

function formatContentType(contentType: string): string {
  return contentType.charAt(0).toUpperCase() + contentType.slice(1);
}

function formatReasons(contentTypes: string[]): string {
  return contentTypes.map(formatContentType).join(', ');
}

export function SourceList({ sources }: SourceListProps) {
  if (sources.length === 0) {
    return null;
  }

  return (
    <section className="source-list" aria-label="Answer sources">
      <h3>Sources</h3>
      <ul>
        {sources.map((source) => (
          <li key={source.ticketId || source.displayId}>
            {source.ticketId ? (
              <Link to={`/tickets/${source.ticketId}`}>{source.displayId}</Link>
            ) : (
              <span>{source.displayId}</span>
            )}
            <span className="source-reasons">Matched: {formatReasons(source.contentTypes)}</span>
            <span className="source-badges">
              {source.contentTypes.map((contentType) => (
                <span key={contentType} className="content-type-badge">
                  {contentType}
                </span>
              ))}
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}
