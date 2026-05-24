'use client';

import Link from 'next/link';
import { useMemo, useState } from 'react';
import { formatDateTime, formatMoney } from '@/lib/format';
import { StatusBadge } from '@/components/ui/status-badge';
import type { BackendServiceRequest } from '@/lib/api/backend';
import type { ServiceRequest } from '@/lib/types';

function normalizeStatus(value: BackendServiceRequest['status']): ServiceRequest['status'] {
  return String(value).toLowerCase() as ServiceRequest['status'];
}

export function RequestsPage({ membershipId, initialRequests, initialMembershipStatus, initialError = null }: {
  membershipId: number;
  initialRequests: BackendServiceRequest[];
  initialMembershipStatus: string | null;
  initialError?: string | null
}) {
  const [requests] = useState<ServiceRequest[]>(initialRequests.map((item) => ({
    id: item.id,
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
  const [membershipStatus] = useState<string | null>(initialMembershipStatus);
  const [error] = useState<string | null>(initialError);

  const canCreate = useMemo(() => membershipStatus === 'active' || membershipStatus === 'blocked', [membershipStatus]);

  if (error) return <div
    className="border rounded-4 border-danger-subtle bg-danger-subtle text-danger-emphasis p-3 mb-0">{error}</div>;

  return (
    <div className="d-grid gap-4">
      <section className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
              <h1 className="h3 mb-2">Сервисные заявки</h1>
            </div>
            <Link
              href={canCreate ? `/memberships/${membershipId}/requests/new` : `/memberships/${membershipId}/requests`}
              className={`btn ${canCreate ? 'btn-primary' : 'btn-outline-secondary disabled'}`}>
              Создать сервисную заявку
            </Link>
          </div>
        </div>
      </section>

      <div className="card border-0 shadow-sm">
        <div className="card-body p-4">
          <div className="d-grid gap-3">
            {requests.length === 0 ? (
              <div className="text-body-secondary">Сервисных заявок пока нет.</div>
            ) : requests.map((request) => (
              <Link href={`/memberships/${membershipId}/requests/${request.id}`} key={request.id}
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
