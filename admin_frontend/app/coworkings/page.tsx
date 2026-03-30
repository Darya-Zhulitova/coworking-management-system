import { requireAdminSession } from '@/lib/auth/session';

export default async function CoworkingsPage() {
  const session = await requireAdminSession();

  return (
    <main className="page">
      <section className="content-card">
        <h1>Coworkings</h1>
        <p className="section-text">Principal type: {session.principalType}</p>
        {session.coworkings.length > 0 ? (
          <ul>
            {session.coworkings.map((coworking) => (
              <li key={coworking.id}>
                {coworking.name} (ID: {coworking.id}, role: {coworking.role})
              </li>
            ))}
          </ul>
        ) : (
          <p className="section-text">No coworkings are available for the current administrator.</p>
        )}
      </section>
    </main>
  );
}
