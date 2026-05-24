'use client';

import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import { useEffect, useState } from 'react';
import type { UserAnalyticsDto, UserQueueSummaryDto } from '@/types/place';
import { formatRublesFromKopecks } from '@/lib/format/money';

type ManagementCard = {
  title: string;
  href: string;
  button: string;
  badgeCount?: number;
};

const REFRESH_INTERVAL_MS = 5000;

function formatBadgeCount(count: number): string {
  return count > 99 ? '99+' : String(count);
}

function QueueButton({ href, label, count }: { href: string; label: string; count: number }) {
  return <Button href={href} className="position-relative">
    {label}
    {count > 0 ? <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">
      {formatBadgeCount(count)}
      <span className="visually-hidden">ожидают обработки</span>
    </span> : null}
  </Button>;
}

export function CoworkingUsersPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: contextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [summary, setSummary] = useState<UserQueueSummaryDto | null>(null);
  const [analytics, setAnalytics] = useState<UserAnalyticsDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const loadDashboardData = (showLoader: boolean) => {
      if (showLoader) setLoading(true);
      Promise.all([
        requestJson<UserQueueSummaryDto>(`/api/coworkings/${coworkingId}/users/summary`),
        requestJson<UserAnalyticsDto>(`/api/coworkings/${coworkingId}/users/analytics`),
      ]).then(([summaryData, analyticsData]) => {
        if (!mounted) return;
        setSummary(summaryData);
        setAnalytics(analyticsData);
        setError(null);
      }).catch((e) => {
        if (!mounted) return;
        setError(e instanceof Error ? e.message : 'Не удалось загрузить пользователей.');
      }).finally(() => {
        if (mounted && showLoader) setLoading(false);
      });
    };

    loadDashboardData(true);
    const intervalId = window.setInterval(() => loadDashboardData(false), REFRESH_INTERVAL_MS);

    return () => {
      mounted = false;
      window.clearInterval(intervalId);
    };
  }, [coworkingId]);

  if (contextLoading || loading) return <FullPageLoader label="Загрузка раздела пользователей..."/>;
  if (contextError || error) return <FullPageError
    message={contextError ?? error ?? 'Не удалось загрузить пользователей.'}/>;
  if (!context || context.coworkingId == null || !summary || !analytics) return <FullPageLoader
    label="Переход на страницу входа..."/>;

  const cards: ManagementCard[] = [
    {
      title: 'Список пользователй',
      href: `/coworkings/${coworkingId}/users/membership-list`,
      button: 'Открыть'
    },
    {
      title: 'Очередь заявок на участие',
      href: `/coworkings/${coworkingId}/users/memberships`,
      button: 'Открыть',
      badgeCount: summary.pendingMemberships
    },
    {
      title: 'Платежные заявки',
      href: `/coworkings/${coworkingId}/users/pay-requests`,
      button: 'Открыть',
      badgeCount: summary.pendingPayRequests
    },
    {
      title: 'Сервисные заявки',
      href: `/coworkings/${coworkingId}/users/service-requests`,
      button: 'Открыть',
      badgeCount: summary.openServiceRequests
    },
  ];

  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}>
    <h2 className="mb-0">Управление коворкингом</h2>
    <Row className="g-4">{cards.map((card) => <Col md={6} lg={3} key={card.title}><Card
      className="content-card h-100"><Card.Body className="d-flex flex-column gap-3">
      <Card.Title as="h3" className="h5 mb-0">{card.title}</Card.Title>
      <div className="mt-auto">{card.badgeCount == null
        ? <Button href={card.href}>{card.button}</Button>
        : <QueueButton href={card.href} label={card.button} count={card.badgeCount}/>}</div>
    </Card.Body></Card></Col>)}</Row>
    <Row className="g-4">
      <Col lg={4}><Card className="content-card h-100"><Card.Body><Card.Title as="h3" className="h4 mb-3">Сводная
        аналитика</Card.Title><Stack gap={2}>
        <div>Общий депозит пользователей: {formatRublesFromKopecks(analytics.totalBalance)}</div>
        <div>Доход за месяц: {formatRublesFromKopecks(analytics.monthlyIncome)}</div>
        <div>Заполненность на месяц: {analytics.monthlyOccupancyPercent}%</div>
        <div>Количество пользователей: {analytics.activeMemberships}</div>
        <div>Ожидают подтверждения: {analytics.pendingMemberships}</div>
      </Stack></Card.Body></Card></Col>
    </Row>
  </Stack></Container></main>;
}
