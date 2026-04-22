'use client';

import Card from 'react-bootstrap/Card';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import Button from 'react-bootstrap/Button';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';

const cards = [
  {
    key: 'info',
    title: 'Информация о коворкинге',
    text: 'Редактирование основных данных коворкинга.',
    hrefSuffix: '/settings/info',
    grant: 'COWORKING_READ'
  },
  {
    key: 'floors',
    title: 'Этажи',
    text: 'Управление этажами и переход к списку мест на отдельной странице.',
    hrefSuffix: '/settings/floors',
    grant: 'FLOOR_READ'
  },
  {
    key: 'place-types',
    title: 'Типы мест',
    text: 'Создание типов мест и привязка каждого типа к одному тарифу.',
    hrefSuffix: '/settings/place-types',
    grant: 'PLACE_TYPE_READ'
  },
  {
    key: 'schedule',
    title: 'Расписание',
    text: 'Настройка недельного расписания, исключений и закрытий мест.',
    hrefSuffix: '/settings/schedule',
    grant: 'SCHEDULE_READ'
  },
  {
    key: 'tariffs',
    title: 'Тарифы',
    text: 'Настройка тарифов, правил скидок и коэффициентов компенсации.',
    hrefSuffix: '/settings/tariffs',
    grant: 'TARIFF_READ'
  },
  {
    key: 'service-request-types',
    title: 'Типы сервисных заявок',
    text: 'Управление шаблонами, которые копируются в новые пользовательские заявки.',
    hrefSuffix: '/settings/service-request-types',
    grant: 'SERVICE_REQUEST_TYPE_READ'
  },
];

export function CoworkingSettingsPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading, errorMessage } = useAppContext({ coworkingId, redirectToLogin: true });
  if (isLoading) return <FullPageLoader label="Загрузка настроек коворкинга..."/>;
  if (errorMessage) return <FullPageError message={errorMessage}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;
  return <main className="page-shell"><Container className="py-4 py-md-5"><h2 className="mb-4">Настройки коворкинга</h2>
    <Stack gap={4}><Row
      className="g-4">{cards.filter((card) => context.grants.includes(card.grant)).map((card) => <Col md={6} lg={4}
                                                                                                      key={card.key}><Card
      className="content-card h-100"><Card.Body className="d-flex flex-column gap-3">
      <div><Card.Title as="h2" className="h4">{card.title}</Card.Title><Card.Text
        className="text-body-secondary mb-0">{card.text}</Card.Text></div>
      <div className="mt-auto"><Button href={`/coworkings/${coworkingId}${card.hrefSuffix}`}>Открыть</Button></div>
    </Card.Body></Card></Col>)}</Row></Stack></Container></main>;
}
