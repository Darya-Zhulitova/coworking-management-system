'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import CardBody from 'react-bootstrap/CardBody';
import CardTitle from 'react-bootstrap/CardTitle';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import ListGroup from 'react-bootstrap/ListGroup';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { requestJson } from '@/lib/client/api';
import type { Coworking } from '@/types/coworking';
import { useAppContext } from '@/features/context/use-app-context';

export function CoworkingsPageClient() {
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({ redirectToLogin: true });
  const [coworkings, setCoworkings] = useState<Coworking[]>([]);
  const [archivedCoworkings, setArchivedCoworkings] = useState<Coworking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const canCreate = useMemo(() => context?.grants.includes('COWORKING_EDIT') ?? false, [context]);

  useEffect(() => {
    if (!context) return;
    let isMounted = true;

    Promise.all([
      requestJson<Coworking[]>('/api/coworkings'),
      requestJson<Coworking[]>('/api/coworkings?archived=true'),
    ])
      .then(([active, archived]) => {
        if (!isMounted) return;
        setCoworkings(active);
        setArchivedCoworkings(archived);
      })
      .catch((error) => {
        if (!isMounted) return;
        setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить коворкинги.');
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [context]);

  if (isContextLoading || (context && isLoading)) {
    return <FullPageLoader label="Загрузка коворкингов..."/>;
  }

  if (contextError) {
    return <FullPageError message={contextError}/>;
  }

  if (!context) {
    return <FullPageLoader label="Переход на страницу входа..."/>;
  }

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5">
        <Stack gap={4}>
          <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
            <h2 className="mb-0">Список коворкингов</h2>
            <Stack direction="horizontal" gap={2} className="flex-wrap">
              {canCreate ? <Link href="/coworkings/new"><Button>Создать коворкинг</Button></Link> : null}
            </Stack>
          </div>

          <Card className="content-card">
            <CardBody>
              {errorMessage ? <Alert variant="danger">{errorMessage}</Alert> : null}
              {coworkings.length > 0 ? (
                <ListGroup variant="flush">
                  {coworkings.map((coworking) => (
                    <ListGroup.Item key={coworking.id} className="px-0 entity-card">
                      <Row className="g-3 align-items-center">
                        <Col md>
                          <div className="fw-semibold fs-5">{coworking.name}</div>
                          <div className="text-body-secondary">{coworking.address}</div>
                          <div className="text-body-secondary small mt-1">{coworking.workingHoursLabel}</div>
                          {coworking.heroTitle ? <div className="mt-2 fw-semibold">{coworking.heroTitle}</div> : null}
                          <div className="mt-2">{coworking.description}</div>
                          <Stack direction="horizontal" gap={2} className="flex-wrap mt-3">
                            <Badge bg={coworking.active ? 'success' : 'secondary'}>
                              {coworking.active ? 'Активен' : 'Неактивен'}
                            </Badge>
                          </Stack>
                        </Col>
                        <Col md="auto">
                          <Button as={Link} href={`/coworkings/${coworking.id}/users`} variant="primary">
                            Открыть
                          </Button>
                        </Col>
                      </Row>
                    </ListGroup.Item>
                  ))}
                </ListGroup>
              ) : (
                <Alert variant="secondary" className="mb-0">Для текущей учетной записи нет доступных
                  коворкингов.</Alert>
              )}
            </CardBody>
          </Card>

          {archivedCoworkings.length > 0 ? (
            <Card className="content-card">
              <CardBody>
                <CardTitle as="h2" className="h4 mb-3">Архив коворкингов</CardTitle>
                <ListGroup variant="flush">
                  {archivedCoworkings.map((coworking) => (
                    <ListGroup.Item key={coworking.id} className="px-0 entity-card">
                      <div className="fw-semibold">{coworking.name}</div>
                      <div className="text-body-secondary">{coworking.address}</div>
                      <div className="text-body-secondary small">{coworking.workingHoursLabel}</div>
                    </ListGroup.Item>
                  ))}
                </ListGroup>
              </CardBody>
            </Card>
          ) : null}
        </Stack>
      </Container>
    </main>
  );
}
