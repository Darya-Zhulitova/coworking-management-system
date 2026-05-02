'use client';

import { ReactNode, useEffect } from 'react';
import { AppHeader } from '@/components/layout/app-header';
import { CoworkingShellProvider } from '@/components/layout/coworking-shell-context';

export function AppShell({ children }: { children: ReactNode }) {
  useEffect(() => {
    import('bootstrap/dist/js/bootstrap.bundle.min.js');
  }, []);

  return (
    <CoworkingShellProvider>
      <div className="bg-body-tertiary min-vh-100">
        <AppHeader/>
        <main className="container py-4 py-lg-5 app-main-shell">{children}</main>
      </div>
    </CoworkingShellProvider>
  );
}
