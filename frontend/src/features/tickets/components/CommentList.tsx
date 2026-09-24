import type { Comment } from '../api/ticketApi';

interface CommentListProps {
  comments: Comment[];
}

export function CommentList({ comments }: CommentListProps) {
  if (comments.length === 0) {
    return <p className="empty-state">No comments yet.</p>;
  }

  return (
    <ul className="comment-list">
      {comments.map((comment) => (
        <li key={comment.id}>
          <p>{comment.content}</p>
          <small>
            {comment.author} · {new Date(comment.createdAt).toLocaleString()}
          </small>
        </li>
      ))}
    </ul>
  );
}
