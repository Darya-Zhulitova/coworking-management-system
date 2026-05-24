'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { requestJson } from '@/lib/client/api';
import type { JoinCoworkingPreview, JoinCoworkingResult } from '@/lib/types';
import { useToast } from '@/components/ui/toast-provider';

interface JoinCoworkingPageClientProps {
  preview: JoinCoworkingPreview;
  joinToken: string;
  isAuthenticated: boolean;
}

export function JoinCoworkingPageClient({ preview, joinToken, isAuthenticated }: JoinCoworkingPageClientProps) {
  const router = useRouter();
  const toast = useToast();
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleJoin() {
    if (!isAuthenticated) {
      router.push(`/login?next=${encodeURIComponent(`/join/${joinToken}`)}`);
      return;
    }

    setIsSubmitting(true);
    try {
      const result = await requestJson<JoinCoworkingResult>(`/api/coworkings/join/${encodeURIComponent(joinToken)}`, {
        method: 'POST',
      });
      if (result.existingMembership) {
        router.replace(`/memberships/${result.membershipId}`);
        router.refresh();
        return;
      }
      const message = result.status === 'active'
        ? 'Вы присоединились к коворкингу.'
        : 'Заявка отправлена, ожидает подтверждения администратора.';
      router.replace(`/memberships/${result.membershipId}?joined=${result.status}`);
      router.refresh();
      toast.success(message);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Не удалось присоединиться к коворкингу.');
    } finally {
      setIsSubmitting(false);
    }
  }

  const buttonLabel = preview.autoApproveMembership ? 'Присоединиться' : 'Отправить запрос на присоединение';
  const imageUrl = preview.imageUrls[0];

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-lg-9 col-xl-8">
        <div className="card border-0 shadow-sm overflow-hidden">
          {imageUrl ? (
            <div className="ratio ratio-21x9 bg-body-secondary">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={imageUrl} alt={preview.name} className="w-100 h-100 object-fit-cover"/>
            </div>
          ) : null}
          <div className="card-body p-4 p-lg-5">
            <div className="d-flex flex-column flex-md-row gap-3 justify-content-between align-items-md-start mb-4">
              <div>
                <div className="text-body-secondary small mb-2">Приглашение в коворкинг</div>
                <h1 className="h2 mb-2">{preview.heroTitle || preview.name}</h1>
                {preview.heroText ? <p className="lead text-body-secondary mb-0">{preview.heroText}</p> : null}
              </div>
              <span
                className={`badge text-bg-${preview.autoApproveMembership ? 'success' : 'warning'} align-self-start`}>
                {preview.autoApproveMembership ? 'Мгновенное присоединение' : 'Требуется подтверждение'}
              </span>
            </div>

            <div className="row g-3 mb-4">
              <div className="col-12 col-md-6">
                <div className="border rounded-4 p-3 h-100 bg-body-tertiary">
                  <div className="small text-body-secondary mb-1">Адрес</div>
                  <div className="fw-semibold">{preview.address}</div>
                </div>
              </div>
              <div className="col-12 col-md-6">
                <div className="border rounded-4 p-3 h-100 bg-body-tertiary">
                  <div className="small text-body-secondary mb-1">Режим работы</div>
                  <div className="fw-semibold">{preview.workingHoursLabel}</div>
                </div>
              </div>
            </div>

            <p className="mb-4">{preview.description}</p>

            {!preview.active ? (
              <div className="alert alert-warning mb-4">
                Коворкинг сейчас неактивен. Присоединение может быть недоступно до включения пространства
                администратором.
              </div>
            ) : null}

            <div className="d-grid d-sm-flex gap-2">
              <button className="btn btn-primary btn-lg" type="button" onClick={handleJoin}
                      disabled={isSubmitting || !preview.active}>
                {isSubmitting ? 'Отправляем...' : buttonLabel}
              </button>
              <button className="btn btn-outline-secondary btn-lg" type="button" onClick={() => router.push('/')}>
                На главную
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
