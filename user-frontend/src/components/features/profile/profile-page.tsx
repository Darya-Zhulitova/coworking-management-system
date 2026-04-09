import Link from 'next/link';
import {currentUser, memberships} from '@/lib/mock-data';
import {StatusBadge} from '@/components/ui/status-badge';

type ProfilePageProps = {
  selectedCoworkingId?: number | null;
};

export function ProfilePage({selectedCoworkingId = null}: ProfilePageProps) {
  return (
    <div className="row g-4">
      <div className="col-12 col-xl-5">
        <div className="card border-0 shadow-sm h-100">
          <div className="card-body p-4 p-lg-5 d-grid gap-4">
            <div>
              <h1 className="h3 mb-2">Профиль</h1>
            </div>
            <form className="d-grid gap-3">
              <div>
                <label className="form-label">Имя</label>
                <input className="form-control" defaultValue={currentUser.name}/>
              </div>
              <div>
                <label className="form-label">Email</label>
                <input className="form-control" defaultValue={currentUser.email} readOnly/>
              </div>
              <div>
                <label className="form-label">О себе</label>
                <textarea className="form-control" rows={4} defaultValue={currentUser.description}/>
              </div>
              <div className="d-flex flex-wrap gap-2">
                <button type="button" className="btn btn-primary">Сохранить</button>
                <button type="button" className="btn btn-outline-danger">Выйти из системы</button>
                {selectedCoworkingId ? (
                  <Link href="/" className="btn btn-outline-secondary">
                    Выйти из коворкинга
                  </Link>
                ) : null}
              </div>
            </form>
          </div>
        </div>
      </div>
      <div className="col-12 col-xl-7">
        <div className="card border-0 shadow-sm h-100">
          <div className="card-body p-4 p-lg-5">
            <div className="mb-4">
              <h2 className="h5 mb-1">Мои коворкинги</h2>
            </div>
            <div className="d-grid gap-3">
              {memberships.map((membership) => {
                const selected = membership.coworkingId === selectedCoworkingId;
                return (
                  <div className={`border rounded-4 p-3 ${selected ? 'border-dark-subtle bg-body-tertiary' : ''}`} key={membership.id}>
                    <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-2">
                      <div>
                        <div className="fw-semibold">{membership.coworkingName}</div>
                        <div className="small text-body-secondary">{membership.address}</div>
                      </div>
                      <StatusBadge status={membership.status}/>
                    </div>
                    <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
                      <div className="small text-body-secondary d-flex flex-wrap gap-2 align-items-center">
                        <span>{membership.scheduleLabel}</span>
                        {selected ? <span className="badge text-bg-dark">Выбран</span> : null}
                      </div>
                      {selected ? (
                        <Link href="/" className="btn btn-sm btn-dark">
                          Выйти
                        </Link>
                      ) : (
                        <Link href={`/coworkings/${membership.coworkingId}`} className="btn btn-sm btn-outline-primary">
                          Перейти
                        </Link>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
