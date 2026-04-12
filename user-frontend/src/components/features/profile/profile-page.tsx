'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { StatusBadge } from '@/components/ui/status-badge';
import { ClientRequestError, requestJson } from '@/lib/client/api';
import { formatMoney } from '@/lib/format';
import type { MembershipSummary, UserCoworkingDetails, UserProfile } from '@/lib/types';

type ProfilePageProps = {
  selectedCoworkingId?: number | null;
};

export function ProfilePage({ selectedCoworkingId = null }: ProfilePageProps) {
  const router = useRouter();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [memberships, setMemberships] = useState<MembershipSummary[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    const requests: Array<Promise<unknown>> = [
      requestJson<UserProfile>('/api/users/me'),
      requestJson<MembershipSummary[]>('/api/users/me/memberships'),
    ];

    if (selectedCoworkingId != null) {
      requests.push(requestJson<UserCoworkingDetails>(`/api/coworkings/${selectedCoworkingId}`));
    }

    Promise.all(requests)
      .then((response) => {
        if (!isMounted) return;
        setProfile(response[0] as UserProfile);
        setMemberships(response[1] as MembershipSummary[]);
      })
      .catch((error) => {
        if (!isMounted) return;
        setErrorMessage(error instanceof ClientRequestError || error instanceof Error ? error.message : 'Не удалось загрузить профиль.');
      });

    return () => {
      isMounted = false;
    };
  }, [selectedCoworkingId]);

  const currentMembership = useMemo(
    () => memberships.find((membership) => membership.coworkingId === selectedCoworkingId) ?? null,
    [memberships, selectedCoworkingId]
  );

  async function handleLogout() {
    await fetch('/api/auth/logout', { method: 'POST' });
    router.replace('/login');
    router.refresh();
  }

  return (
    <div className="d-grid gap-4">
      <div className="row g-4">
        <div className="col-12 col-xl-5">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4 p-lg-5 d-grid gap-4">
              <div>
                <h2 className="h4 mb-2">Личные данные</h2>
                <p className="text-body-secondary mb-0">Информация учетной записи и базовые настройки профиля.</p>
              </div>

              {errorMessage ? <div className="alert alert-danger mb-0">{errorMessage}</div> : null}

              <div className="d-grid gap-3">
                <div>
                  <label className="form-label">Имя</label>
                  <input className="form-control" value={profile?.name ?? ''} readOnly/>
                </div>
                <div>
                  <label className="form-label">Электронная почта</label>
                  <input className="form-control" value={profile?.email ?? ''} readOnly/>
                </div>
                <div>
                  <label className="form-label">О себе</label>
                  <textarea className="form-control" rows={4} value={profile?.description ?? ''} readOnly/>
                </div>
              </div>

              <div className="d-flex flex-wrap gap-2">
                <button type="button" className="btn btn-outline-danger" onClick={handleLogout}>
                  Выйти из системы
                </button>
                {selectedCoworkingId ? (
                  <Link href="/" className="btn btn-outline-secondary">
                    К списку коворкингов
                  </Link>
                ) : null}
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-7">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-4 p-lg-5 d-grid gap-4">
              <div>
                <h2 className="h4 mb-2">Подключенные коворкинги</h2>
                <p className="text-body-secondary mb-0">Здесь показаны все коворкинги, к которым у вас есть доступ.</p>
              </div>

              <div className="list-group list-group-flush">
                {memberships.map((membership) => {
                  const isCurrent = membership.coworkingId === selectedCoworkingId;

                  return (
                    <div className="list-group-item px-0 py-3" key={membership.id}>
                      <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
                        <div>
                          <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
                            <div className="fw-semibold">{membership.coworkingName}</div>
                            <StatusBadge status={membership.status}/>
                          </div>
                          <div className="text-body-secondary small">{membership.address}</div>
                          <div className="text-body-secondary small mt-1">{membership.scheduleLabel}</div>
                          <div className="small mt-2">Баланс: <span
                            className="fw-semibold">{formatMoney(membership.balance)}</span></div>
                        </div>

                        <div className="d-flex flex-wrap gap-2">
                          {isCurrent ? (
                            <button type="button" className="btn btn-sm btn-outline-secondary" disabled>
                              Текущий коворкинг
                            </button>
                          ) : (
                            <Link href={`/coworkings/${membership.coworkingId}`} className="btn btn-sm btn-primary">
                              Перейти
                            </Link>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
