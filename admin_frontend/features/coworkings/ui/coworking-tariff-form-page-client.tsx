'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import type { TariffDto } from '@/types/place';
import { kopecksToRublesInput, rublesInputToKopecks } from '@/lib/format/money';

const defaultForm = {
  name: '',
  pricePerDay: '0',
  fullRefundHoursBefore: '24',
  lateCancellationRefundPercent: '0',
  cancellationCompensationCoefficient: '0',
  dayClosureCompensationCoefficient: '0',
  membershipBlockCompensationCoefficient: '0',
  active: true,
};

type TariffFormState = typeof defaultForm;

function toFormState(tariff: TariffDto): TariffFormState {
  return {
    name: tariff.name,
    pricePerDay: kopecksToRublesInput(tariff.pricePerDay),
    fullRefundHoursBefore: String(tariff.fullRefundHoursBefore),
    lateCancellationRefundPercent: String(tariff.lateCancellationRefundPercent),
    cancellationCompensationCoefficient: String(tariff.cancellationCompensationCoefficient),
    dayClosureCompensationCoefficient: String(tariff.dayClosureCompensationCoefficient),
    membershipBlockCompensationCoefficient: String(tariff.membershipBlockCompensationCoefficient),
    active: tariff.active,
  };
}

function normalizePayload(form: TariffFormState) {
  return {
    name: form.name.trim(),
    pricePerDay: rublesInputToKopecks(form.pricePerDay),
    fullRefundHoursBefore: Number(form.fullRefundHoursBefore),
    lateCancellationRefundPercent: Number(form.lateCancellationRefundPercent),
    cancellationCompensationCoefficient: Number(form.cancellationCompensationCoefficient),
    dayClosureCompensationCoefficient: Number(form.dayClosureCompensationCoefficient),
    membershipBlockCompensationCoefficient: Number(form.membershipBlockCompensationCoefficient),
    active: form.active,
  };
}

function validateTariffForm(form: TariffFormState): string | null {
  if (!form.name.trim()) return 'Название тарифа обязательно.';
  const pricePerDay = rublesInputToKopecks(form.pricePerDay);
  if (!Number.isFinite(pricePerDay) || pricePerDay < 0) return 'Цена за день должна быть корректной суммой в рублях и не меньше нуля.';
  const fullRefundHoursBefore = Number(form.fullRefundHoursBefore);
  if (!Number.isInteger(fullRefundHoursBefore) || fullRefundHoursBefore < 0) return 'Часы до полного возврата должны быть не меньше нуля.';
  const lateCancellationRefundPercent = Number(form.lateCancellationRefundPercent);
  if (!Number.isInteger(lateCancellationRefundPercent) || lateCancellationRefundPercent < 0 || lateCancellationRefundPercent > 100) {
    return 'Процент возврата при поздней отмене должен быть от 0 до 100.';
  }

  const coefficients = [
    ['Коэффициент компенсации при отмене', Number(form.cancellationCompensationCoefficient)],
    ['Коэффициент компенсации при закрытии дня', Number(form.dayClosureCompensationCoefficient)],
    ['Коэффициент компенсации при блокировке участия', Number(form.membershipBlockCompensationCoefficient)],
  ] as const;
  for (const [label, value] of coefficients) {
    if (!Number.isFinite(value) || value < 0) return `${label} должен быть не меньше нуля.`;
  }

  return null;
}

export function CoworkingTariffFormPageClient({ coworkingId, tariffId }: { coworkingId: number; tariffId?: number }) {
  const isEditMode = tariffId != null;
  const router = useRouter();
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [form, setForm] = useState<TariffFormState>(defaultForm);
  const [currentVersion, setCurrentVersion] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(isEditMode);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const canManage = useMemo(() => context?.grants.includes('TARIFF_EDIT') ?? false, [context]);

  useEffect(() => {
    if (!isEditMode) return;
    let mounted = true;
    requestJson<TariffDto>(`/api/coworkings/${coworkingId}/tariffs/${tariffId}`)
      .then((tariff) => {
        if (!mounted) return;
        setForm(toFormState(tariff));
        setCurrentVersion(tariff.version);
      })
      .catch((error) => {
        if (mounted) setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить тариф.');
      })
      .finally(() => {
        if (mounted) setIsLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [coworkingId, isEditMode, tariffId]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationMessage = validateTariffForm(form);
    if (validationMessage) {
      setErrorMessage(validationMessage);
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const payload = normalizePayload(form);
      const response = await requestJson<TariffDto>(
        isEditMode ? `/api/coworkings/${coworkingId}/tariffs/${tariffId}` : `/api/coworkings/${coworkingId}/tariffs`,
        {
          method: isEditMode ? 'PUT' : 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        },
      );
      setForm(toFormState(response));
      setCurrentVersion(response.version);
      setSuccessMessage(isEditMode ? 'Тариф обновлен.' : 'Тариф создан.');
      if (!isEditMode) {
        router.replace(`/coworkings/${coworkingId}/settings/tariffs/${response.id}`);
        router.refresh();
      }
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : isEditMode ? 'Не удалось обновить тариф.' : 'Не удалось создать тариф.');
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isContextLoading || isLoading) return <FullPageLoader
    label={isEditMode ? 'Загрузка тарифа...' : 'Загрузка формы тарифа...'}/>;
  if (contextError) return <FullPageError message={contextError}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;
  if (!canManage) return <FullPageError message="У вас нет прав на изменение тарифов."/>;
  if (errorMessage && isEditMode && currentVersion == null) return <FullPageError message={errorMessage}/>;

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5">
        <Stack gap={4}>
          <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
            <div><h2 className="mb-2">{isEditMode ? 'Редактировать тариф' : 'Создать тариф'}</h2><p
              className="mb-0 text-body-secondary">Настройка стоимости, правил возврата, компенсаций и порогов
              ценообразования.</p></div>
            <Stack direction="horizontal" gap={2} className="flex-wrap">
              {currentVersion != null ? <Badge bg="info">Версия {currentVersion}</Badge> : null}
              <Button variant="outline-secondary"
                      onClick={() => router.push(`/coworkings/${coworkingId}/settings/tariffs`)}>К тарифам</Button>
            </Stack>
          </div>
          {successMessage ? <Alert variant="success" className="mb-0">{successMessage}</Alert> : null}
          {errorMessage && (!isEditMode || currentVersion != null) ?
            <Alert variant="danger" className="mb-0">{errorMessage}</Alert> : null}
          <Card className="content-card">
            <Card.Body>
              <Form onSubmit={handleSubmit}>
                <Stack gap={3}>
                  <Form.Group controlId="tariff-name">
                    <Form.Label>Название тарифа</Form.Label>
                    <Form.Control value={form.name}
                                  onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                                  required/>
                  </Form.Group>
                  <Row className="g-3">
                    <Col md={6}>
                      <Form.Group controlId="price-per-day">
                        <Form.Label>Цена за день, ₽</Form.Label>
                        <Form.Control type="number" min={0} step="0.01" value={form.pricePerDay}
                                      onChange={(event) => setForm((current) => ({
                                        ...current,
                                        pricePerDay: event.target.value
                                      }))}/>
                      </Form.Group>
                    </Col>
                    <Col md={6}>
                      <Form.Group controlId="full-refund-hours-before">
                        <Form.Label>Часов до полного возврата</Form.Label>
                        <Form.Control type="number" min={0} value={form.fullRefundHoursBefore}
                                      onChange={(event) => setForm((current) => ({
                                        ...current,
                                        fullRefundHoursBefore: event.target.value
                                      }))}/>
                      </Form.Group>
                    </Col>
                    <Col md={6}>
                      <Form.Group controlId="late-cancellation-refund-percent">
                        <Form.Label>Процент возврата при поздней отмене</Form.Label>
                        <Form.Control type="number" min={0} max={100} value={form.lateCancellationRefundPercent}
                                      onChange={(event) => setForm((current) => ({
                                        ...current,
                                        lateCancellationRefundPercent: event.target.value
                                      }))}/>
                      </Form.Group>
                    </Col>
                    <Col md={4}>
                      <Form.Group controlId="cancellation-compensation-coefficient">
                        <Form.Label>Коэффициент компенсации при отмене</Form.Label>
                        <Form.Control type="number" min={0} step="0.0001"
                                      value={form.cancellationCompensationCoefficient}
                                      onChange={(event) => setForm((current) => ({
                                        ...current,
                                        cancellationCompensationCoefficient: event.target.value
                                      }))}/>
                      </Form.Group>
                    </Col>
                    <Col md={4}>
                      <Form.Group controlId="day-closure-compensation-coefficient">
                        <Form.Label>Коэффициент компенсации при закрытии дня</Form.Label>
                        <Form.Control type="number" min={0} step="0.0001" value={form.dayClosureCompensationCoefficient}
                                      onChange={(event) => setForm((current) => ({
                                        ...current,
                                        dayClosureCompensationCoefficient: event.target.value
                                      }))}/>
                      </Form.Group>
                    </Col>
                    <Col md={4}>
                      <Form.Group controlId="membership-block-compensation-coefficient">
                        <Form.Label>Коэффициент компенсации при блокировке участия</Form.Label>
                        <Form.Control type="number" min={0} step="0.0001"
                                      value={form.membershipBlockCompensationCoefficient}
                                      onChange={(event) => setForm((current) => ({
                                        ...current,
                                        membershipBlockCompensationCoefficient: event.target.value
                                      }))}/>
                      </Form.Group>
                    </Col>
                  </Row>
                  <Form.Check
                    type="switch"
                    id="tariff-active"
                    label="Активен"
                    checked={form.active}
                    onChange={(event) => setForm((current) => ({ ...current, active: event.target.checked }))}
                  />
                  <div className="d-flex gap-2 justify-content-end flex-wrap">
                    <Button variant="outline-secondary" type="button"
                            onClick={() => router.push(`/coworkings/${coworkingId}/settings/tariffs`)}>
                      Отмена
                    </Button>
                    <Button type="submit" disabled={isSubmitting}>
                      {isSubmitting ? (isEditMode ? 'Сохранение...' : 'Создание...') : (isEditMode ? 'Сохранить тариф' : 'Создать тариф')}
                    </Button>
                  </div>
                </Stack>
              </Form>
            </Card.Body>
          </Card>
        </Stack>
      </Container>
    </main>
  );
}
