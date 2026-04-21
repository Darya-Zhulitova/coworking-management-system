'use client';

import { useTheme } from '@/components/theme/theme-provider';

export function ThemeToggleButton() {
  const { theme, isReady, toggleTheme } = useTheme();
  const isDark = theme === 'dark';
  const iconClassName = isDark ? 'bi bi-sun-fill' : 'bi bi-moon-stars-fill';

  return (
    <button
      type="button"
      className="btn btn-link nav-link d-inline-flex align-items-center justify-content-center p-0 border-0 text-body-emphasis text-decoration-none"
      onClick={toggleTheme}
      aria-label={isDark ? 'Переключить на светлую тему' : 'Переключить на тёмную тему'}
      title={isDark ? 'Светлая тема' : 'Тёмная тема'}
      disabled={!isReady}
    >
      <i aria-hidden="true" className={`${iconClassName} fs-5 lh-1`}/>
    </button>
  );
}
