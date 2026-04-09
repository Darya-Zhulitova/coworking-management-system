import type {Metadata} from 'next';
import 'bootstrap/dist/css/bootstrap.min.css';
import './globals.css';
import {AppShell} from '@/components/layout/app-shell';

export const metadata: Metadata = {
  title: 'Coworking Resident',
  description: 'Пользовательский фронтенд коворкинг-системы',
};

export default function RootLayout({children}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ru">
    <body>
    <AppShell>{children}</AppShell>
    </body>
    </html>
  );
}
