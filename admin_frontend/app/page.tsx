import Link from 'next/link';
import { getAdminSession } from '@/lib/auth/session';
import { env } from '@/lib/config/env';

export default async function HomePage() {
  const session = await getAdminSession();

  return (
    <main className="page">
      <section className="hero-card">
        <h1>Coworking Admin</h1>
        <p>
          API base URL: <code>{env.apiBaseUrl}</code>
        </p>
        <p>{session ? `Signed in as ${session.principalType === 'SUPERADMIN' ? `superadmin #${session.superAdminId}` : `administrator #${session.adminUserId}`}.` : 'Sign in to access the admin workspace.'}</p>
      </section>

      <nav className="nav-card" aria-label="Primary navigation">
        <Link className="nav-link" href={session ? '/dashboard' : '/login'}>
          {session ? 'Dashboard' : 'Login'}
        </Link>
        <Link className="nav-link" href="/coworkings">
          Coworkings
        </Link>
      </nav>
    </main>
  );
}
