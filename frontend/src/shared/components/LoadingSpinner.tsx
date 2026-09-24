interface LoadingSpinnerProps {
  label?: string;
}

export function LoadingSpinner({ label = 'Loading' }: LoadingSpinnerProps) {
  return (
    <div className="loading-spinner" role="status" aria-live="polite" aria-label={label}>
      {label}…
    </div>
  );
}
