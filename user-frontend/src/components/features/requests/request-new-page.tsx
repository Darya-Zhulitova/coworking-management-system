import {getMembership, getServiceRequestTypes} from '@/lib/mock-data';
import {formatMoney} from '@/lib/format';

export function RequestNewPage({coworkingId}: { coworkingId: number }) {
  const membership = getMembership(coworkingId);
  const requestTypes = getServiceRequestTypes(coworkingId);

  if (membership?.status !== 'active') {
    return <div className="alert alert-warning mb-0">Новая заявка доступна только для active membership.</div>;
  }

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-xl-8">
        <div className="card border-0 shadow-sm">
          <div className="card-body p-4 p-lg-5 d-grid gap-4">
            <div>
              <h1 className="h3 mb-2">Создание заявки</h1>
            </div>
            <form className="d-grid gap-3">
              <div>
                <label className="form-label">Тип заявки</label>
                <select className="form-select">
                  {requestTypes.map((type) => (
                    <option key={type.id}>{type.name} · {formatMoney(type.cost)}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="form-label">Название</label>
                <input className="form-control" placeholder="Например, Подготовить переговорную к созвону"/>
              </div>
              <div>
                <label className="form-label">Описание</label>
                <textarea className="form-control" rows={5} placeholder="Подробно опишите задачу или проблему."/>
              </div>
              <div className="d-flex justify-content-end">
                <button type="button" className="btn btn-primary btn-lg">Создать заявку</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
