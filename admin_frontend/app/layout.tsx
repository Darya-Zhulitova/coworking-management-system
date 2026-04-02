import 'bootstrap/dist/css/bootstrap.min.css';
import './globals.css';
import type { Metadata, Viewport } from 'next';
import { ServiceWorkerRegistration } from '@/components/service-worker-registration';

export const metadata: Metadata = {
  title: {
    default: 'Coworking Admin',
    template: '%s | Coworking Admin',
  },
  description: 'Admin panel for coworking management.',
  applicationName: 'Coworking Admin',
  manifest: '/manifest.webmanifest',
  appleWebApp: {
    capable: true,
    statusBarStyle: 'default',
    title: 'Coworking Admin',
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
    <html lang="en">
      <body className="app-body">
        <ServiceWorkerRegistration />
        {children}
      </body>
    </html>
  );
}
