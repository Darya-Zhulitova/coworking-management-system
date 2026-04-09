import {ReactNode} from 'react';
import {AppHeader} from '@/components/layout/app-header';

export function AppShell({children}: { children: ReactNode }) {
  return (
    <div className="bg-body-tertiary min-vh-100">
      <AppHeader/>
      <main className="container py-4 py-lg-5 app-main-shell">{children}</main>
    </div>
  );
}
