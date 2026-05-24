'use client';

import { createContext, ReactNode, useCallback, useContext, useMemo, useRef, useState } from 'react';

type ToastVariant = 'success' | 'danger' | 'info' | 'warning';

type ToastMessage = {
  id: string;
  title: string;
  message: string;
  variant: ToastVariant;
};

type ToastContextValue = {
  success: (message: string, title?: string) => void;
  error: (message: string, title?: string) => void;
  info: (message: string, title?: string) => void;
  warning: (message: string, title?: string) => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

const variantTitles: Record<ToastVariant, string> = {
  success: 'Успешно',
  danger: 'Ошибка',
  info: 'Информация',
  warning: 'Внимание',
};

const variantClasses: Record<ToastVariant, string> = {
  success: 'border-success',
  danger: 'border-danger',
  info: 'border-info',
  warning: 'border-warning',
};

const headerClasses: Record<ToastVariant, string> = {
  success: 'text-success',
  danger: 'text-danger',
  info: 'text-info',
  warning: 'text-warning',
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const toastIdRef = useRef(0);

  const dismiss = useCallback((id: string) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const show = useCallback((variant: ToastVariant, message: string, title = variantTitles[variant]) => {
    toastIdRef.current += 1;
    const id = `${Date.now()}-${toastIdRef.current}`;

    setToasts((current) => [...current, { id, title, message, variant }]);
    window.setTimeout(() => dismiss(id), 5000);
  }, [dismiss]);

  const value = useMemo<ToastContextValue>(() => ({
    success: (message, title) => show('success', message, title),
    error: (message, title) => show('danger', message, title),
    info: (message, title) => show('info', message, title),
    warning: (message, title) => show('warning', message, title),
  }), [show]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-container position-fixed end-0 p-3 z-3" style={{ top: '72px' }}>
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={`toast show shadow-sm ${variantClasses[toast.variant]}`}
            role="alert"
            aria-live="assertive"
            aria-atomic="true"
          >
            <div className="toast-header">
              <strong className={`me-auto ${headerClasses[toast.variant]}`}>{toast.title}</strong>
              <button type="button" className="btn-close" onClick={() => dismiss(toast.id)} aria-label="Закрыть"/>
            </div>
            <div className="toast-body">{toast.message}</div>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used inside ToastProvider.');
  }
  return context;
}
