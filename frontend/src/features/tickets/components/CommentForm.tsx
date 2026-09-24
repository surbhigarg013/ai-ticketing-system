import { FormEvent, useState } from 'react';

interface CommentFormProps {
  onSubmit: (content: string, author: string) => Promise<void>;
}

export function CommentForm({ onSubmit }: CommentFormProps) {
  const [content, setContent] = useState('');
  const [author, setAuthor] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!content.trim() || !author.trim()) {
      return;
    }
    setSubmitting(true);
    try {
      await onSubmit(content.trim(), author.trim());
      setContent('');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="comment-form">
      <label>
        Author
        <input
          value={author}
          onChange={(e) => setAuthor(e.target.value)}
          required
          maxLength={100}
        />
      </label>
      <label>
        Comment
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          required
          rows={3}
        />
      </label>
      <button type="submit" disabled={submitting || !content.trim() || !author.trim()}>
        Add Comment
      </button>
    </form>
  );
}
