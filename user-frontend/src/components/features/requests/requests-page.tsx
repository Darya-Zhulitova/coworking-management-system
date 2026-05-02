'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { formatDateTime, formatMoney } from '@/lib/format';
import { StatusBadge } from '@/components/ui/status-badge';
import { ClientRequestError, requestJson } from '@/lib/client/api';
import type { BackendBalanceDetails, BackendServiceRequest } from '@/lib/api/backend';
import type { ServiceRequest } from '@/lib/types';

function normalizeStatus(value: BackendServiceRequest['status']): ServiceRequest['status'] {
  return String(value).toLowerCase() as ServiceRequest['status'];
}

export function RequestsPage({ coworkingId }: { coworkingId: number }) {
  const [requests, setRequests] = useState<ServiceRequest[]>([]);
  const [membershipStatus, setMembershipStatus] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    Promise.all([
      requestJson<BackendServiceRequest[]>(`/api/coworkings/${coworkingId}/service-requests`),
      requestJson<BackendBalanceDetails>(`/api/coworkings/${coworkingId}/balance`),
    ])
      .then(([requestItems, balance]) => {
        if (!active) return;
        setRequests(requestItems.map((item) => ({
          id: item.id,
          coworkingId: item.coworkingId,
          membershipId: item.membershipId,
          typeId: item.typeId,
          name: item.name,
          typeName: item.typeName,
          cost: item.cost,
          status: normalizeStatus(item.status),
          createdAt: item.createdAt,
          updatedAt: item.updatedAt,
          resolvedAt: item.resolvedAt ?? undefined,
        })));
        setMembershipStatus(String(balance.membershipStatus).toLowerCase());
        setError(null);
      })
      .catch((err: unknown) => {
        if (!active) return;
        setError(err instanceof ClientRequestError ? err.message : 'Не удалось загрузить заявки.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [coworkingId]);

  const canCreate = useMemo(() => membershipStatus === 'active' || membershipStatus === 'blocked', [membershipStatus]);

  if (loading) return <div className="alert alert-light border mb-0">Загрузка заявок...</div>;
  if (error) return <div className="alert alert-danger mb-0">{error}</div>;

  return (
    <div className="d-grid gap-4">
      <section className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
              <h1 className="h3 mb-2">Заявки</h1>
            </div>
            <Link href={canCreate ? `/coworkings/${coworkingId}/requests/new` : `/coworkings/${coworkingId}/requests`}
                  className={`btn ${canCreate ? 'btn-primary' : 'btn-outline-secondary disabled'}`}>
              Создать заявку
            </Link>
          </div>
        </div>
      </section>

      <div className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <div className="d-grid gap-3">
            {requests.length === 0 ? (
              <div className="text-body-secondary">Заявок пока нет.</div>
            ) : requests.map((request) => (
              <Link href={`/coworkings/${coworkingId}/requests/${request.id}`} key={request.id}
                    className="border rounded-4 p-3 text-reset text-decoration-none">
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
                  {request.cost !== 0 && <span>Стоимость: {formatMoney(request.cost)}</span>}
                </div>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
