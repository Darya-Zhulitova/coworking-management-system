'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { StatusBadge } from '@/components/ui/status-badge';
import { formatMoney } from '@/lib/format';
import { ClientRequestError, requestJson } from '@/lib/client/api';
import type { MembershipSummary } from '@/lib/types';

export function Dashboard() {
  const [memberships, setMemberships] = useState<MembershipSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    requestJson<MembershipSummary[]>('/api/users/me/memberships')
      .then((data) => {
        if (!isMounted) return;
        setMemberships(data);
      })
      .catch((error) => {
        if (!isMounted) return;
        setErrorMessage(error instanceof ClientRequestError || error instanceof Error ? error.message : 'Не удалось загрузить список коворкингов.');
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <div className="d-grid gap-4">
      <section className="card border-0 shadow-sm">
        <div className="card-body p-4 p-lg-5">
          <h1 className="h3 mb-2">Мои коворкинги</h1>
          <p className="text-body-secondary mb-0">Выберите коворкинг, чтобы перейти к бронированиям, балансу и сервисным
            заявкам.</p>
        </div>
      </section>

      {isLoading ? <div className="alert alert-secondary mb-0">Загрузка списка коворкингов...</div> : null}
      {errorMessage ? <div className="alert alert-danger mb-0">{errorMessage}</div> : null}

      {!isLoading && !errorMessage ? (
        <div className="row g-4">
          {memberships.length === 0 ? (
            <div className="col-12">
              <div className="card border-0 shadow-sm">
                <div className="card-body p-4">
                  <h2 className="h5 mb-2">У вас пока нет подключенных коворкингов</h2>
                  <p className="text-body-secondary mb-0">Чтобы начать работу, дождитесь приглашения или подтверждения
                    доступа от администратора.</p>
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
                  <div className="d-flex justify-content-between align-items-center gap-3 mt-auto pt-3 border-top">
                    <div>
                      <div className="text-body-secondary small">Баланс</div>
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
      ) : null}
    </div>
  );
}
