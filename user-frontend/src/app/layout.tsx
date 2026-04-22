import type { Metadata } from 'next';
import Script from 'next/script';
import 'bootstrap-icons/font/bootstrap-icons.css';
import './globals.css';
import { AppShell } from '@/components/layout/app-shell';
import { ThemeProvider } from '@/components/theme/theme-provider';

export const metadata: Metadata = {
  title: 'SpaceBooking',
  description: 'Пользовательский кабинет системы SpaceBooking',
  icons: {
    icon: '/brand-icon.svg',
    shortcut: '/brand-icon.svg',
    apple: '/brand-icon.svg',
  },
};

const themeInitScript = `(function () {
  try {
    var key = 'spacebooking-theme';
    var saved = localStorage.getItem(key);
    var theme = saved === 'dark' || saved === 'light' ? saved : 'light';
    document.documentElement.setAttribute('data-bs-theme', theme);
  } catch (error) {
    document.documentElement.setAttribute('data-bs-theme', 'light');
  }
})();`;

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ru" suppressHydrationWarning>
    <head>
      <link rel="stylesheet" href="/css/custom.css"/>
    </head>
    <body>
    <Script id="theme-init" strategy="beforeInteractive">
      {themeInitScript}
    </Script>
    <ThemeProvider>
      <AppShell>{children}</AppShell>
    </ThemeProvider>
    </body>
    </html>
  );
}
