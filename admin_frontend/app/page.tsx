import Link from 'next/link';

export default function HomePage() {
  return (
    <main className="page">
      <section className="hero-card">
        <h1>Admin Frontend</h1>
      </section>

      <nav className="nav-card" aria-label="Primary navigation">
        <Link className="nav-link" href="/login">
          Login
        </Link>
        <Link className="nav-link" href="/dashboard">
          Dashboard
        </Link>
        <Link className="nav-link" href="/coworkings">
          Coworkings
        </Link>
      </nav>
    </main>
  );
}
