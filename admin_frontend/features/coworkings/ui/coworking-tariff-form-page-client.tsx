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
  minBookingDays: '1',
  fullRefundHoursBefore: '24',
  lateCancellationRefundPercent: '0',
  cancellationCompensationCoefficient: '0',
  dayClosureCompensationCoefficient: '0',
  membershipBlockCompensationCoefficient: '0',
  discountRules: [{ thresholdQuantity: '2', discountPercent: '10' }],
  active: true,
};

type TariffFormState = typeof defaultForm;

function toFormState(tariff: TariffDto): TariffFormState {
  return {
    name: tariff.name,
    pricePerDay: kopecksToRublesInput(tariff.pricePerDay),
    minBookingDays: String(tariff.minBookingDays),
    fullRefundHoursBefore: String(tariff.fullRefundHoursBefore),
    lateCancellationRefundPercent: String(tariff.lateCancellationRefundPercent),
    cancellationCompensationCoefficient: String(tariff.cancellationCompensationCoefficient),
    dayClosureCompensationCoefficient: String(tariff.dayClosureCompensationCoefficient),
    membershipBlockCompensationCoefficient: String(tariff.membershipBlockCompensationCoefficient),
    discountRules: tariff.discountRules.length > 0
      ? tariff.discountRules.map((rule) => ({
        thresholdQuantity: String(rule.thresholdQuantity),
        discountPercent: String(rule.discountPercent)
      }))
      : [],
    active: tariff.active,
  };
}

function normalizePayload(form: TariffFormState) {
  return {
    name: form.name.trim(),
    pricePerDay: rublesInputToKopecks(form.pricePerDay),
    minBookingDays: Number(form.minBookingDays),
    fullRefundHoursBefore: Number(form.fullRefundHoursBefore),
    lateCancellationRefundPercent: Number(form.lateCancellationRefundPercent),
    cancellationCompensationCoefficient: Number(form.cancellationCompensationCoefficient),
    dayClosureCompensationCoefficient: Number(form.dayClosureCompensationCoefficient),
    membershipBlockCompensationCoefficient: Number(form.membershipBlockCompensationCoefficient),
    discountRules: form.discountRules
      .filter((rule) => rule.thresholdQuantity.trim() !== '' && rule.discountPercent.trim() !== '')
      .map((rule) => ({
        thresholdQuantity: Number(rule.thresholdQuantity),
        discountPercent: Number(rule.discountPercent),
      })),
    active: form.active,
  };
}

function validateTariffForm(form: TariffFormState): string | null {
  if (!form.name.trim()) return 'Название тарифа обязательно.';
  const pricePerDay = rublesInputToKopecks(form.pricePerDay);
  if (!Number.isFinite(pricePerDay) || pricePerDay < 0) return 'Цена за день должна быть корректной суммой в рублях и не меньше нуля.';
  const minBookingDays = Number(form.minBookingDays);
  if (!Number.isInteger(minBookingDays) || minBookingDays < 1) return 'Минимальное число дней бронирования должно быть не меньше 1.';
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

  let previousThreshold: number | null = null;
  let previousDiscount: number | null = null;
  for (const [index, rule] of form.discountRules.entries()) {
    const threshold = Number(rule.thresholdQuantity);
    const discount = Number(rule.discountPercent);
    if (!Number.isInteger(threshold) || threshold < 1) {
      return `Правило скидки №${index + 1}: порог количества должен быть не меньше 1.`;
    }
    if (!Number.isInteger(discount) || discount < 0 || discount > 100) {
      return `Правило скидки №${index + 1}: процент скидки должен быть от 0 до 100.`;
    }
    if (previousThreshold != null && threshold <= previousThreshold) {
      return 'Правила скидок должны быть упорядочены по строго возрастающему порогу количества.';
    }
    if (previousDiscount != null && discount <= previousDiscount) {
      return 'Процент скидки должен строго расти вместе с порогом количества.';
    }
    previousThreshold = threshold;
    previousDiscount = discount;
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

  function updateDiscountRule(index: number, field: 'thresholdQuantity' | 'discountPercent', value: string) {
    setForm((current) => ({
      ...current,
      discountRules: current.discountRules.map((rule, ruleIndex) => (ruleIndex === index ? {
        ...rule,
        [field]: value
      } : rule)),
    }));
  }

  function addDiscountRule() {
    setForm((current) => ({
      ...current,
      discountRules: [...current.discountRules, { thresholdQuantity: '', discountPercent: '' }]
    }));
  }

  function removeDiscountRule(index: number) {
    setForm((current) => ({
      ...current,
      discountRules: current.discountRules.filter((_, ruleIndex) => ruleIndex !== index)
    }));
  }

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
      setSuccessMessage(isEditMode ? 'Тариф обновлён.' : 'Тариф создан.');
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
              скидок.</p></div>
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
                      <Form.Group controlId="min-booking-days">
                        <Form.Label>Минимальное число дней бронирования</Form.Label>
                        <Form.Control type="number" min={1} value={form.minBookingDays}
                                      onChange={(event) => setForm((current) => ({
                                        ...current,
                                        minBookingDays: event.target.value
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
                  <Card bg="light" className="border-0">
                    <Card.Body>
                      <Stack gap={3}>
                        <div className="d-flex justify-content-between align-items-center">
                          <div>
                            <div className="fw-semibold">Правила скидок</div>
                            <div className="text-body-secondary small">Правила должны идти по возрастанию порога
                              количества и процента скидки.
                            </div>
                          </div>
                          <Button variant="outline-primary" size="sm" onClick={addDiscountRule}>Добавить
                            правило</Button>
                        </div>
                        {form.discountRules.length === 0 ?
                          <div className="text-body-secondary small">Правила скидок не настроены.</div> : null}
                        {form.discountRules.map((rule, index) => (
                          <Row className="g-3 align-items-end" key={`discount-rule-${index}`}>
                            <Col md={5}>
                              <Form.Group controlId={`discount-threshold-${index}`}>
                                <Form.Label>Порог количества</Form.Label>
                                <Form.Control type="number" min={1} value={rule.thresholdQuantity}
                                              onChange={(event) => updateDiscountRule(index, 'thresholdQuantity', event.target.value)}/>
                              </Form.Group>
                            </Col>
                            <Col md={5}>
                              <Form.Group controlId={`discount-percent-${index}`}>
                                <Form.Label>Процент скидки</Form.Label>
                                <Form.Control type="number" min={0} max={100} value={rule.discountPercent}
                                              onChange={(event) => updateDiscountRule(index, 'discountPercent', event.target.value)}/>
                              </Form.Group>
                            </Col>
                            <Col md={2}>
                              <Button variant="outline-danger" onClick={() => removeDiscountRule(index)}
                                      disabled={form.discountRules.length === 1 && rule.thresholdQuantity === '' && rule.discountPercent === ''}>
                                Удалить
                              </Button>
                            </Col>
                          </Row>
                        ))}
                      </Stack>
                    </Card.Body>
                  </Card>
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
