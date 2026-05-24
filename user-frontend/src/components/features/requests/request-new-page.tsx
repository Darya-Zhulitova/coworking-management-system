'use client';

import { useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { formatMoney } from '@/lib/format';
import { ClientRequestError, requestJson } from '@/lib/client/api';
import type { BackendServiceRequest, BackendServiceRequestTypeOption } from '@/lib/api/backend';
import { useToast } from '@/components/ui/toast-provider';

export function RequestNewPage({ membershipId, initialMembershipStatus, initialRequestTypes, initialError = null }: {
  membershipId: number;
  initialMembershipStatus: string | null;
  initialRequestTypes: BackendServiceRequestTypeOption[];
  initialError?: string | null
}) {
  const router = useRouter();
  const toast = useToast();
  const [membershipStatus] = useState<string | null>(initialMembershipStatus);
  const [requestTypes] = useState<BackendServiceRequestTypeOption[]>(initialRequestTypes);
  const [selectedTypeId, setSelectedTypeId] = useState<number | null>(initialRequestTypes[0]?.id ?? null);
  const [name, setName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(initialError);

  const selectedType = useMemo(() => requestTypes.find((item) => item.id === selectedTypeId) ?? null, [requestTypes, selectedTypeId]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedTypeId) {
      toast.warning('Выберите тип сервисной заявки.');
      return;
    }
    if (!name.trim()) {
      toast.warning('Введите название сервисной заявки.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const created = await requestJson<BackendServiceRequest>(`/api/memberships/${membershipId}/service-requests`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ typeId: selectedTypeId, name: name.trim() }),
      });
      toast.success('Сервисная заявка создана.');
      router.push(`/memberships/${membershipId}/requests/${created.id}`);
      router.refresh();
    } catch (err: unknown) {
      toast.error(err instanceof ClientRequestError ? err.message : 'Не удалось создать сервисную заявку.');
    } finally {
      setSubmitting(false);
    }
  }

  if (error && requestTypes.length === 0) return <div
    className="border rounded-4 border-danger-subtle bg-danger-subtle text-danger-emphasis p-3 mb-0">{error}</div>;
  if (membershipStatus === 'pending') return <div
    className="border rounded-4 border-warning-subtle bg-warning-subtle text-warning-emphasis p-3 mb-0">Новая сервисная
    заявка недоступна, пока доступ к коворкингу ожидает подтверждения.</div>;

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-xl-8">
        <div className="card border-0 shadow-sm">
          <div className="card-body p-4 p-lg-5 d-grid gap-4">
            <div>
              <h1 className="h3 mb-2">Создание сервисной заявки</h1>
            </div>
            <form className="d-grid gap-3" onSubmit={handleSubmit}>
              <div>
                <label className="form-label">Тип сервисной заявки</label>
                <select className="form-select" value={selectedTypeId ?? ''}
                        onChange={(event) => setSelectedTypeId(Number(event.target.value))}>
                  {requestTypes.map((type) => (
                    <option key={type.id} value={type.id}>{type.name} · {formatMoney(type.cost)}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="form-label">Название</label>
                <input className="form-control" value={name} onChange={(event) => setName(event.target.value)}
                       placeholder="Например, Подготовить переговорную к созвону" maxLength={255}/>
              </div>
              <div className="border rounded-4 p-3 bg-body-tertiary">
                <div className="small text-body-secondary mb-1">Стоимость</div>
                <div className="fw-semibold">{selectedType ? formatMoney(selectedType.cost) : '—'}</div>
              </div>
              <div className="d-flex justify-content-end">
                <button type="submit" className="btn btn-primary btn-lg"
                        disabled={submitting || requestTypes.length === 0}>
                  {submitting ? 'Создание...' : 'Создать сервисную заявку'}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
