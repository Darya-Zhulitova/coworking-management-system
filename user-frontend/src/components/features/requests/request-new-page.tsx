'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { formatMoney } from '@/lib/format';
import { ClientRequestError, requestJson } from '@/lib/client/api';
import type { BackendBalanceDetails, BackendServiceRequest, BackendServiceRequestTypeOption } from '@/lib/api/backend';

export function RequestNewPage({ coworkingId }: { coworkingId: number }) {
  const router = useRouter();
  const [membershipStatus, setMembershipStatus] = useState<string | null>(null);
  const [requestTypes, setRequestTypes] = useState<BackendServiceRequestTypeOption[]>([]);
  const [selectedTypeId, setSelectedTypeId] = useState<number | null>(null);
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    Promise.all([
      requestJson<BackendBalanceDetails>(`/api/coworkings/${coworkingId}/balance`),
      requestJson<BackendServiceRequestTypeOption[]>(`/api/coworkings/${coworkingId}/service-request-types`),
    ])
      .then(([balance, types]) => {
        if (!active) return;
        setMembershipStatus(String(balance.membershipStatus).toLowerCase());
        setRequestTypes(types);
        setSelectedTypeId(types[0]?.id ?? null);
        setError(null);
      })
      .catch((err: unknown) => {
        if (!active) return;
        setError(err instanceof ClientRequestError ? err.message : 'Не удалось загрузить данные для заявки.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [coworkingId]);

  const selectedType = useMemo(() => requestTypes.find((item) => item.id === selectedTypeId) ?? null, [requestTypes, selectedTypeId]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedTypeId) {
      setError('Выберите тип заявки.');
      return;
    }
    if (!name.trim()) {
      setError('Введите название заявки.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const created = await requestJson<BackendServiceRequest>('/api/service-requests', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ coworkingId, typeId: selectedTypeId, name: name.trim() }),
      });
      router.push(`/coworkings/${coworkingId}/requests/${created.id}`);
      router.refresh();
    } catch (err: unknown) {
      setError(err instanceof ClientRequestError ? err.message : 'Не удалось создать заявку.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <div className="alert alert-light border mb-0">Загрузка формы...</div>;
  if (membershipStatus === 'pending') return <div className="alert alert-warning mb-0">Новая заявка недоступна для
    membership в статусе ожидания.</div>;

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-xl-8">
        <div className="card border-0 shadow-sm">
          <div className="card-body p-4 p-lg-5 d-grid gap-4">
            <div>
              <h1 className="h3 mb-2">Создание заявки</h1>
            </div>
            <form className="d-grid gap-3" onSubmit={handleSubmit}>
              <div>
                <label className="form-label">Тип заявки</label>
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
              {error && <div className="alert alert-danger mb-0">{error}</div>}
              <div className="d-flex justify-content-end">
                <button type="submit" className="btn btn-primary btn-lg"
                        disabled={submitting || requestTypes.length === 0}>
                  {submitting ? 'Создание...' : 'Создать заявку'}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
