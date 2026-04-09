import {getLedgerEntries, getMembership, getPayRequests} from '@/lib/mock-data';
import {formatDateTime, formatMoney} from '@/lib/format';
import {StatusBadge} from '@/components/ui/status-badge';

export function BalancePage({coworkingId}: { coworkingId: number }) {
  const membership = getMembership(coworkingId);
  const ledger = getLedgerEntries(coworkingId);
  const requests = getPayRequests(coworkingId);
  const actionsLocked = membership?.status !== 'active';

  return (
    <div className="d-grid gap-4">
      <section className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
              <h1 className="h3 mb-2">Баланс</h1>
            </div>
            <div className="text-end">
              <div className="text-body-secondary small">Доступно</div>
              <div className="display-6 fw-semibold">{formatMoney(membership?.balance ?? 0)}</div>
            </div>
          </div>
          <div className="d-flex flex-wrap gap-2 mt-4">
            <button type="button" className="btn btn-primary" disabled={actionsLocked}>Пополнить</button>
            <button type="button" className="btn btn-outline-secondary" disabled={actionsLocked}>Вывести</button>
          </div>
          {actionsLocked && <div className="small text-body-secondary mt-3">Новые финансовые запросы доступны только для active membership.</div>}
        </div>
      </section>

      <div className="row g-4">
        <div className="col-12 col-xl-7">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4">
              <div className="d-flex justify-content-between align-items-center gap-3 mb-3">
                <h2 className="h5 mb-0">История операций</h2>
                <select className="form-select form-select-sm w-auto">
                  <option>Все типы</option>
                  <option>Пополнения и вывод</option>
                  <option>Бронирования</option>
                  <option>Компенсации и возвраты</option>
                </select>
              </div>
              <div className="table-responsive">
                <table className="table align-middle mb-0">
                  <thead>
                  <tr>
                    <th>Дата</th>
                    <th>Операция</th>
                    <th>Комментарий</th>
                    <th className="text-end">Сумма</th>
                  </tr>
                  </thead>
                  <tbody>
                  {ledger.map((entry) => (
                    <tr key={entry.id}>
                      <td>{formatDateTime(entry.timestamp)}</td>
                      <td>
                        <div className="fw-semibold">{entry.name}</div>
                        <div className="small text-body-secondary">{entry.type}</div>
                      </td>
                      <td>{entry.comment}</td>
                      <td className={`text-end fw-semibold ${entry.amount >= 0 ? 'text-success' : 'text-danger'}`}>{formatMoney(entry.amount)}</td>
                    </tr>
                  ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-5">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4">
              <h2 className="h5 mb-3">Мои финансовые запросы</h2>
              <div className="d-grid gap-3">
                {requests.map((request) => (
                  <div className="border rounded-4 p-3" key={request.id}>
                    <div className="d-flex justify-content-between align-items-start gap-3 mb-2">
                      <div className="fw-semibold">{formatMoney(request.amount)}</div>
                      <StatusBadge status={request.status}/>
                    </div>
                    <div className="small text-body-secondary mb-2">{formatDateTime(request.createdAt)}</div>
                    <div>{request.userComment}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
