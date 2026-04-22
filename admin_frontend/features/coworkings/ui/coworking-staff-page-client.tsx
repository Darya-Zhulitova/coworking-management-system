'use client';

import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';

export function CoworkingStaffPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading, errorMessage } = useAppContext({ coworkingId, redirectToLogin: true });
  if (isLoading) return <FullPageLoader label="Загрузка раздела сотрудников..."/>;
  if (errorMessage) return <FullPageError message={errorMessage}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;
  const cards = [
    {
      key: 'roles',
      title: 'Роли',
      text: 'Создание и редактирование ролей коворкинга.',
      href: `/coworkings/${coworkingId}/roles`,
      visible: context.grants.includes('ROLE_READ')
    },
    {
      key: 'list',
      title: 'Список сотрудников',
      text: 'Управление назначениями сотрудников и активацией доступа.',
      href: `/coworkings/${coworkingId}/staff/list`,
      visible: context.grants.includes('ACCESS_READ')
    },
  ].filter((card) => card.visible);
  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}><Row
    className="g-4">{cards.map((card) => <Col md={6} key={card.key}><Card
    className="content-card h-100"><Card.Body className="d-flex flex-column gap-3">
    <div><Card.Title as="h2" className="h4">{card.title}</Card.Title><Card.Text
      className="text-body-secondary mb-0">{card.text}</Card.Text></div>
    <div className="mt-auto"><Button href={card.href}>Открыть</Button></div>
  </Card.Body></Card></Col>)}</Row></Stack></Container></main>;
}
