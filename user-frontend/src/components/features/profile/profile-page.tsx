'use client';

import Link from 'next/link';
import { type FormEvent, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { StatusBadge } from '@/components/ui/status-badge';
import { ClientRequestError, requestJson } from '@/lib/client/api';
import { formatMoney } from '@/lib/format';
import type { MembershipSummary, UserProfile } from '@/lib/types';
import { useToast } from '@/components/ui/toast-provider';

type ProfilePageProps = {
  selectedMembershipId?: number | null;
  initialProfile: UserProfile | null;
  initialMemberships: MembershipSummary[];
  initialError?: string | null;
};

export function ProfilePage({
                              selectedMembershipId = null,
                              initialProfile,
                              initialMemberships,
                              initialError = null
                            }: ProfilePageProps) {
  const router = useRouter();
  const toast = useToast();
  const [profile, setProfile] = useState<UserProfile | null>(initialProfile);
  const [memberships] = useState<MembershipSummary[]>(initialMemberships);
  const [errorMessage, setErrorMessage] = useState<string | null>(initialError);
  const [isSaving, setIsSaving] = useState(false);
  const [form, setForm] = useState({
    name: initialProfile?.name ?? '',
    description: initialProfile?.description ?? ''
  });

  const currentMembership = useMemo(
    () => memberships.find((membership) => membership.id === selectedMembershipId) ?? null,
    [memberships, selectedMembershipId]
  );

  async function handleSaveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    setErrorMessage(null);

    try {
      const updated = await requestJson<UserProfile>('/api/user/me', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      });
      setProfile(updated);
      setForm({ name: updated.name ?? '', description: updated.description ?? '' });
      toast.success('Профиль обновлен.');
    } catch (error) {
      toast.error(error instanceof ClientRequestError || error instanceof Error ? error.message : 'Не удалось обновить профиль.');
    } finally {
      setIsSaving(false);
    }
  }

  async function handleLogout() {
    await fetch('/api/user/logout', { method: 'POST' });
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

              {errorMessage ? <div
                className="border rounded-4 border-danger-subtle bg-danger-subtle text-danger-emphasis p-3 mb-0">{errorMessage}</div> : null}


              <form className="d-grid gap-3" onSubmit={handleSaveProfile}>
                <div>
                  <label className="form-label">Имя</label>
                  <input
                    className="form-control"
                    value={form.name}
                    onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                    maxLength={255}
                    required
                  />
                </div>
                <div>
                  <label className="form-label">Электронная почта</label>
                  <input className="form-control" value={profile?.email ?? ''} readOnly/>
                </div>
                <div>
                  <label className="form-label">О себе</label>
                  <textarea
                    className="form-control"
                    rows={4}
                    value={form.description}
                    onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
                    maxLength={1000}
                  />
                </div>
                <div className="d-flex flex-wrap gap-2">
                  <button type="submit" className="btn btn-primary" disabled={isSaving || !form.name.trim()}>
                    {isSaving ? 'Сохраняем...' : 'Сохранить профиль'}
                  </button>
                  <button type="button" className="btn btn-outline-danger" onClick={handleLogout}>
                    Выйти из системы
                  </button>
                  {selectedMembershipId ? (
                    <Link href="/" className="btn btn-outline-secondary">
                      К списку коворкингов
                    </Link>
                  ) : null}
                </div>
              </form>
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
                  const isCurrent = membership.id === selectedMembershipId;

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
                            <Link href={`/memberships/${membership.id}`} className="btn btn-sm btn-primary">
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
