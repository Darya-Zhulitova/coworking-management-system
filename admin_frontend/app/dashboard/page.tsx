import { LogoutButton } from '@/components/logout-button';
import { requireAdminSession } from '@/lib/auth/session';

export default async function DashboardPage() {
  const session = await requireAdminSession();
  const subjectLabel = session.principalType === 'SUPERADMIN'
    ? `superadmin #${session.superAdminId}`
    : `administrator #${session.adminUserId}`;

  return (
    <main className="page">
      <section className="content-card dashboard-card">
        <div className="page-actions">
          <div>
            <h1>Dashboard</h1>
            <p className="section-text">Signed in as {subjectLabel}.</p>
            <p className="section-text">Principal type: {session.principalType}</p>
            <p className="section-text">Available coworkings: {session.coworkings.length}</p>
          </div>
          <LogoutButton />
        </div>

        <div className="section-block">
          <h2>Granted global actions</h2>
          {session.grantedGlobalActions.length > 0 ? (
            <ul>
              {session.grantedGlobalActions.map((action) => (
                <li key={action}>{action}</li>
              ))}
            </ul>
          ) : (
            <p className="section-text">No global actions are available for the current subject.</p>
          )}
        </div>

        {session.coworkings.length > 0 ? (
          <div className="section-block">
            <h2>Accessible coworkings</h2>
            <ul>
              {session.coworkings.map((coworking) => (
                <li key={coworking.id}>
                  {coworking.name} (ID: {coworking.id}, role: {coworking.role})
                </li>
              ))}
            </ul>
          </div>
        ) : (
          <p className="section-text">No coworkings are directly assigned to this subject.</p>
        )}
      </section>
    </main>
  );
}
