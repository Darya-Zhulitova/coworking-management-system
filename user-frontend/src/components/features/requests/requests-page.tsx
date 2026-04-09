import Link from 'next/link';
import {getMembership, getServiceRequests} from '@/lib/mock-data';
import {formatDateTime, formatMoney} from '@/lib/format';
import {StatusBadge} from '@/components/ui/status-badge';

export function RequestsPage({coworkingId}: { coworkingId: number }) {
  const membership = getMembership(coworkingId);
  const requests = getServiceRequests(coworkingId);
  const canCreate = membership?.status === 'active';

  return (
    <div className="d-grid gap-4">
      <section className="d-flex flex-wrap justify-content-between align-items-start gap-3">
        <div>
          <h1 className="h3 mb-2">Заявки</h1>
        </div>
        <Link href={canCreate ? `/coworkings/${coworkingId}/requests/new` : `/coworkings/${coworkingId}/requests`} className={`btn ${canCreate ? 'btn-primary' : 'btn-outline-secondary disabled'}`}>
          Создать заявку
        </Link>
      </section>

      <div className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <div className="d-grid gap-3">
            {requests.map((request) => (
              <Link href={`/coworkings/${coworkingId}/requests/${request.id}`} key={request.id} className="border rounded-4 p-3 text-reset text-decoration-none">
                <div className="d-flex justify-content-between align-items-start gap-3 mb-2">
                  <div>
                    <div className="fw-semibold">{request.name}</div>
                    <div className="text-body-secondary small">{request.typeName}</div>
                  </div>
                  <StatusBadge status={request.status}/>
                </div>
                <div className="d-flex flex-wrap gap-3 small text-body-secondary">
                  <span>Создано: {formatDateTime(request.createdAt)}</span>
                  <span>Обновлено: {formatDateTime(request.updatedAt)}</span>
                  <span>Стоимость: {formatMoney(request.cost)}</span>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
