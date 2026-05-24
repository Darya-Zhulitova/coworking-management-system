'use client';

import { useEffect, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Card from 'react-bootstrap/Card';
import CardBody from 'react-bootstrap/CardBody';
import CardTitle from 'react-bootstrap/CardTitle';
import Container from 'react-bootstrap/Container';
import Stack from 'react-bootstrap/Stack';
import { CoworkingEditForm } from '@/components/coworking-edit-form';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import type { Coworking } from '@/types/coworking';

export function CoworkingDetailsPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [coworking, setCoworking] = useState<Coworking | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    requestJson<Coworking>(`/api/coworkings/${coworkingId}`).then((coworkingData) => {
      if (!isMounted) return;
      setCoworking(coworkingData);
    }).catch((error) => {
      if (isMounted) setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить коворкинг.');
    }).finally(() => {
      if (isMounted) setIsLoading(false);
    });
    return () => {
      isMounted = false;
    };
  }, [coworkingId]);

  if (isContextLoading || isLoading) return <FullPageLoader label="Загрузка данных о коворкинге..."/>;
  if (contextError) return <FullPageError message={contextError}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;
  if (errorMessage) return <FullPageError message={errorMessage}/>;
  if (!coworking) return <FullPageError message="Коворкинг не найден."/>;

  const canEdit = context.grants.includes('COWORKING_EDIT');
  const canViewAccess = context.grants.includes('ACCESS_READ');

  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}><h2
    className="mb-0">{coworking.name}</h2><Card
    className="content-card"><CardBody>
    <h2>Предпросмотр информации о коворкинге</h2>
    <div className="mb-3 text-body-secondary">{coworking.address}</div>
    <div className="mb-3 fw-semibold">{coworking.workingHoursLabel}</div>
    {coworking.heroTitle ?
      <div className="mb-2 fs-5 fw-semibold">{coworking.heroTitle}</div> : null}{coworking.heroText ?
    <div className="mb-3 text-body-secondary">{coworking.heroText}</div> : null}
    <div className="mb-3">{coworking.description}</div>
    {coworking.imageUrls.length > 0 ? <div className="mb-3">
      <div className="fw-semibold mb-2">Фотографии коворкинга</div>
      <Stack direction="horizontal" gap={2} className="flex-wrap">{coworking.imageUrls.map((imageUrl, index) =>
        <img key={`${coworking.id}-${index}`} src={imageUrl} alt={`Фото коворкинга ${index + 1}`}
             style={{ width: 180, height: 101, objectFit: 'cover', borderRadius: 12 }}/>)}</Stack>
    </div> : null}
    <Stack direction="horizontal" gap={2} className="flex-wrap mb-3"><Badge bg="secondary">ID
      коворкинга: {coworking.id}</Badge><Badge
      bg={coworking.active ? 'success' : 'secondary'}>{coworking.active ? 'Активен' : 'Неактивен'}</Badge><Badge
      bg={coworking.archived ? 'dark' : 'info'}>{coworking.archived ? 'В архиве' : 'Показывается'}</Badge><Badge
      bg={coworking.autoApproveMembership ? 'success' : 'warning'}>{coworking.autoApproveMembership ? 'Автоподтверждение включено' : 'Подтверждение участия вручную'}</Badge>
      <Badge
        bg={coworking.floorMapEnabled ? 'primary' : 'secondary'}>{coworking.floorMapEnabled ? 'Карты этажей включены' : 'Бронирование без карт этажей'}</Badge>
    </Stack></CardBody></Card>{canEdit ?
    <Card className="content-card"><CardBody><CardTitle as="h2" className="h4 mb-3">Редактирование
      коворкинга</CardTitle><CoworkingEditForm coworking={coworking}/></CardBody></Card> :
    <Alert variant="secondary" className="mb-0">Эта учетная запись может просматривать данные коворкинга, но не
      может изменять его конфигурацию.</Alert>}</Stack></Container></main>;
}
