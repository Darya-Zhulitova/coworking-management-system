import {getRequestMessages, getServiceRequest} from '@/lib/mock-data';
import {formatDateTime, formatMoney} from '@/lib/format';
import {StatusBadge} from '@/components/ui/status-badge';

function bubbleClass(authorType: 'USER' | 'ADMIN' | 'SYSTEM'): string {
  if (authorType === 'USER') return 'bg-primary-subtle';
  if (authorType === 'ADMIN') return 'bg-body-tertiary';
  return 'bg-warning-subtle';
}

export function RequestDetailPage({coworkingId, requestId}: { coworkingId: number; requestId: number }) {
  const request = getServiceRequest(coworkingId, requestId);
  const messages = getRequestMessages(requestId);

  if (!request) {
    return <div className="alert alert-warning mb-0">Заявка не найдена.</div>;
  }

  const canReply = request.status !== 'resolved' && request.status !== 'rejected';

  return (
    <div className="row g-4">
      <div className="col-12 col-xl-4">
        <div className="card border-0 shadow-sm h-100">
          <div className="card-body p-4 d-grid gap-4">
            <div>
              <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
                <h1 className="h4 mb-0">{request.name}</h1>
                <StatusBadge status={request.status}/>
              </div>
              <div className="text-body-secondary small">{request.typeName}</div>
            </div>
            <div className="border rounded-4 p-3">
              <div className="text-body-secondary small">Создано</div>
              <div className="fw-semibold">{formatDateTime(request.createdAt)}</div>
            </div>
            <div className="border rounded-4 p-3">
              <div className="text-body-secondary small">Стоимость</div>
              <div className="fw-semibold">{formatMoney(request.cost)}</div>
              <div className="small text-body-secondary mt-2">Переход в resolved выполняется только после успешного списания стоимости.</div>
            </div>
          </div>
        </div>
      </div>
      <div className="col-12 col-xl-8">
        <div className="card border-0 shadow-sm h-100">
          <div className="card-body p-4 d-grid gap-4">
            <div className="d-grid gap-3">
              {messages.map((message) => (
                <div key={message.id} className={`border rounded-4 p-3 ${bubbleClass(message.authorType)}`}>
                  <div className="d-flex justify-content-between gap-3 mb-2">
                    <div className="fw-semibold">{message.authorName}</div>
                    <div className="small text-body-secondary">{formatDateTime(message.timestamp)}</div>
                  </div>
                  <div>{message.text}</div>
                  {message.readAt && <div className="small text-body-secondary mt-2">Прочитано {formatDateTime(message.readAt)}</div>}
                </div>
              ))}
            </div>
            <form className="d-grid gap-3">
              <textarea className="form-control" rows={4} placeholder="Введите сообщение" disabled={!canReply}/>
              <div className="d-flex justify-content-end">
                <button type="button" className="btn btn-primary" disabled={!canReply}>Отправить сообщение</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
