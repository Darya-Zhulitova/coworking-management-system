import 'bootstrap/dist/css/bootstrap.min.css';
import './globals.css';
import type { Metadata, Viewport } from 'next';
import { ServiceWorkerRegistration } from '@/components/service-worker-registration';
import { AdminNavbar } from '@/components/admin-navbar';

export const metadata: Metadata = {
  title: {
    default: 'SpaceBooking Manager',
    template: '%s | SpaceBooking Manager',
  },
  description: 'Панель управления коворкингами.',
  applicationName: 'SpaceBooking Manager',
  manifest: '/manifest.webmanifest',
  appleWebApp: {
    capable: true,
    statusBarStyle: 'default',
    title: 'SpaceBooking Manager',
  },
  icons: {
    icon: [
      { url: '/icon-192x192.png', sizes: '192x192', type: 'image/png' },
      { url: '/icon-512x512.png', sizes: '512x512', type: 'image/png' },
    ],
    apple: [{ url: '/apple-icon.png', sizes: '180x180', type: 'image/png' }],
  },
};

export const viewport: Viewport = {
  themeColor: '#0d6efd',
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ru">
    <body className="app-body">
    <ServiceWorkerRegistration/>
    <AdminNavbar/>
    {children}
    </body>
    </html>
  );
}
