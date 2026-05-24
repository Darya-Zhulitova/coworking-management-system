'use client';

type LoadingOverlayProps = {
  message: string;
  ariaLabel?: string;
};

export function LoadingOverlay({ message, ariaLabel = message }: LoadingOverlayProps) {
  return (
    <div
      className="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center bg-body bg-opacity-75"
      style={{ zIndex: 2000 }}
      aria-live="polite"
      aria-busy="true"
    >
      <div className="text-center">
        <div className="spinner-border text-primary" role="status" aria-label={ariaLabel}/>
        <div className="mt-3 fw-medium text-body">{message}</div>
      </div>
    </div>
  );
}
