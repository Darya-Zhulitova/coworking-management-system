'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Stack from 'react-bootstrap/Stack';
import Table from 'react-bootstrap/Table';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import type { TariffDiscountRuleDto, TariffDto } from '@/types/place';
import { formatRublesFromKopecks } from '@/lib/format/money';

function formatRules(rules: TariffDiscountRuleDto[]) {
  if (rules.length === 0) return 'Правила скидок не заданы';
  return rules.map((rule) => `${rule.thresholdQuantity}+ → ${rule.discountPercent}%`).join(', ');
}

export function CoworkingTariffsPageClient({ coworkingId }: { coworkingId: number }) {
  const router = useRouter();
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [tariffs, setTariffs] = useState<TariffDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<number | null>(null);
  const canManage = useMemo(() => context?.grants.includes('TARIFF_EDIT') ?? false, [context]);

  const load = useCallback(async () => {
    const nextTariffs = await requestJson<TariffDto[]>(`/api/coworkings/${coworkingId}/tariffs`);
    setTariffs(nextTariffs);
  }, [coworkingId]);

  useEffect(() => {
    let mounted = true;
    load()
      .catch((error) => {
        if (mounted) setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить тарифы.');
      })
      .finally(() => {
        if (mounted) setIsLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [load]);

  async function toggleItem(item: TariffDto) {
    setSavingId(item.id);
    setErrorMessage(null);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/tariffs/${item.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: item.name,
          pricePerDay: item.pricePerDay,
          minBookingDays: item.minBookingDays,
          fullRefundHoursBefore: item.fullRefundHoursBefore,
          lateCancellationRefundPercent: item.lateCancellationRefundPercent,
          cancellationCompensationCoefficient: item.cancellationCompensationCoefficient,
          dayClosureCompensationCoefficient: item.dayClosureCompensationCoefficient,
          membershipBlockCompensationCoefficient: item.membershipBlockCompensationCoefficient,
          discountRules: item.discountRules,
          active: !item.active,
        }),
      });
      await load();
      setSubmitMessage(item.active ? 'Тариф деактивирован.' : 'Тариф активирован.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось обновить тариф.');
    } finally {
      setSavingId(null);
    }
  }

  if (isContextLoading || isLoading) return <FullPageLoader label="Загрузка тарифов..."/>;
  if (contextError) return <FullPageError message={contextError}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5">
        <Stack gap={4}>
          <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
            <div><h2 className="mb-2">Тарифы</h2><p className="mb-0 text-body-secondary">Управление тарифами, правилами
              скидок и версиями конфигурации для пользовательского домена.</p></div>
            {canManage ? <Button onClick={() => router.push(`/coworkings/${coworkingId}/settings/tariffs/new`)}>Создать
              тариф</Button> : null}
          </div>
          {submitMessage ? <Alert variant="success" className="mb-0">{submitMessage}</Alert> : null}
          {errorMessage ? <Alert variant="danger" className="mb-0">{errorMessage}</Alert> : null}
          <Card className="content-card">
            <Card.Body>
              <Card.Title as="h2" className="h4 mb-3">Каталог тарифов</Card.Title>
              <Table responsive hover className="align-middle mb-0">
                <thead>
                <tr>
                  <th>Название</th>
                  <th>Версия</th>
                  <th>Стоимость</th>
                  <th>Возврат</th>
                  <th>Правила скидок</th>
                  <th>Статус</th>
                  {canManage ? <th className="text-end">Действия</th> : null}
                </tr>
                </thead>
                <tbody>
                {tariffs.length === 0 ? (
                  <tr>
                    <td colSpan={canManage ? 7 : 6} className="text-center text-body-secondary py-4">Тарифы пока не
                      созданы.
                    </td>
                  </tr>
                ) : tariffs.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <div className="fw-semibold">{item.name}</div>
                      <div className="text-body-secondary small">Минимум дней: {item.minBookingDays} дн.</div>
                    </td>
                    <td>
                      <Badge bg="info">v{item.version}</Badge>
                    </td>
                    <td>
                      <div>{formatRublesFromKopecks(item.pricePerDay)} в день</div>
                      <div className="text-body-secondary small">Коэф.
                        отмены {item.cancellationCompensationCoefficient}</div>
                      <div className="text-body-secondary small">Коэф. закрытия
                        дня {item.dayClosureCompensationCoefficient}</div>
                    </td>
                    <td>
                      <div>{item.fullRefundHoursBefore}h for full refund</div>
                      <div className="text-body-secondary small">Поздний
                        возврат: {item.lateCancellationRefundPercent}%
                      </div>
                      <div className="text-body-secondary small">Коэф.
                        блокировки {item.membershipBlockCompensationCoefficient}</div>
                    </td>
                    <td>{formatRules(item.discountRules)}</td>
                    <td>
                      <Stack direction="horizontal" gap={2} className="flex-wrap">
                        <Badge
                          bg={item.active ? 'success' : 'secondary'}>{item.active ? 'Активен' : 'Неактивен'}</Badge>
                        <Badge bg={item.archived ? 'dark' : 'light'}
                               text={item.archived ? undefined : 'dark'}>{item.archived ? 'В архиве' : 'Показывается'}</Badge>
                      </Stack>
                    </td>
                    {canManage ? (
                      <td className="text-end">
                        <Stack direction="horizontal" gap={2} className="justify-content-end flex-wrap">
                          <Button variant="outline-primary" size="sm"
                                  onClick={() => router.push(`/coworkings/${coworkingId}/settings/tariffs/${item.id}`)}>
                            Изменить
                          </Button>
                          {!item.archived ? (
                            <Button variant="outline-secondary" size="sm" disabled={savingId === item.id}
                                    onClick={() => toggleItem(item)}>
                              {item.active ? 'Деактивировать' : 'Активировать'}
                            </Button>
                          ) : null}
                        </Stack>
                      </td>
                    ) : null}
                  </tr>
                ))}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </Stack>
      </Container>
    </main>
  );
}
