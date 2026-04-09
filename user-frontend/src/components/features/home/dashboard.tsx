import Link from 'next/link';
import {memberships} from '@/lib/mock-data';
import {StatusBadge} from '@/components/ui/status-badge';
import {formatMoney} from '@/lib/format';

export function Dashboard() {
  return (
    <div className="d-grid gap-4">
      <section className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <h1 className="h3 mb-2">Выбор коворкинга</h1>
        </div>
      </section>

      <div className="row g-4">
        {memberships.length === 0 ? (
          <div className="col-12">
            <div className="card border-0 shadow-sm">
              <div className="card-body p-4">
                <h2 className="h5 mb-2">У вас пока нет ни одного коворкинга</h2>
                <p className="text-body-secondary mb-0">Для добавления коворкинга потребуется ссылка или приглашение администратора.</p>
              </div>
            </div>
          </div>
        ) : memberships.map((membership) => (
          <div className="col-12 col-lg-6" key={membership.id}>
            <div className="card h-100 border-0 shadow-sm">
              <div className="card-body d-flex flex-column gap-3 p-4">
                <div className="d-flex justify-content-between align-items-start gap-3">
                  <div>
                    <h2 className="h5 mb-1">{membership.coworkingName}</h2>
                    <div className="text-body-secondary small">{membership.address}</div>
                  </div>
                  <StatusBadge status={membership.status}/>
                </div>
                <div className="small text-body-secondary">{membership.scheduleLabel}</div>
                <div className="d-flex justify-content-between align-items-center gap-3 mt-auto">
                  <div>
                    <div className="text-body-secondary small">Баланс в коворкинге</div>
                    <div className="h5 mb-0">{formatMoney(membership.balance)}</div>
                  </div>
                  <Link href={`/coworkings/${membership.coworkingId}`} className="btn btn-primary">
                    Открыть
                  </Link>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
