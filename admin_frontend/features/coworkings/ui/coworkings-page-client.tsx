'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import CardBody from 'react-bootstrap/CardBody';
import CardText from 'react-bootstrap/CardText';
import CardTitle from 'react-bootstrap/CardTitle';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import ListGroup from 'react-bootstrap/ListGroup';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import { CoworkingCreateForm } from '@/components/coworking-create-form';
import { LogoutButton } from '@/components/logout-button';
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

  const canCreate = useMemo(() => context?.grants.includes('COWORKING_CREATE') ?? false, [context]);

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
        setErrorMessage(error instanceof Error ? error.message : 'Unable to load coworkings.');
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [context]);

  if (isContextLoading || (context && isLoading)) return <FullPageLoader label="Loading coworkings..." />;
  if (contextError) return <FullPageError message={contextError} />;
  if (!context) return <FullPageLoader label="Redirecting to login..." />;

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5">
        <Stack gap={4}>
          <Card className="content-card">
            <CardBody>
              <Stack direction="horizontal" className="justify-content-between align-items-start gap-3 flex-wrap">
                <div>
                  <CardTitle as="h1" className="mb-2">Coworkings</CardTitle>
                  <CardText className="mb-1">Signed in as <strong>{context.name}</strong></CardText>
                  <CardText className="mb-0 text-body-secondary">{context.email}</CardText>
                </div>
                <LogoutButton />
              </Stack>
            </CardBody>
          </Card>

          {canCreate ? (
            <Card className="content-card">
              <CardBody>
                <CardTitle as="h2" className="h4 mb-3">Create coworking</CardTitle>
                <CoworkingCreateForm />
              </CardBody>
            </Card>
          ) : null}

          <Card className="content-card">
            <CardBody>
              <CardTitle as="h2" className="h4 mb-3">Accessible coworkings</CardTitle>
              {errorMessage ? <Alert variant="danger">{errorMessage}</Alert> : null}
              {coworkings.length > 0 ? (
                <ListGroup variant="flush">
                  {coworkings.map((coworking) => (
                    <ListGroup.Item key={coworking.id} className="px-0 entity-card">
                      <Row className="g-3 align-items-center">
                        <Col md>
                          <div className="fw-semibold fs-5">{coworking.name}</div>
                          <div className="text-body-secondary">ID: {coworking.id}</div>
                          <Stack direction="horizontal" gap={2} className="flex-wrap mt-2">
                            <Badge bg={coworking.active ? 'success' : 'secondary'}>{coworking.active ? 'Active' : 'Inactive'}</Badge>
                          </Stack>
                        </Col>
                        <Col md="auto">
                          <Stack direction="horizontal" gap={2} className="flex-wrap">
                            <Button as={Link} href={`/coworkings/${coworking.id}/dashboard`} variant="primary">Open</Button>
                          </Stack>
                        </Col>
                      </Row>
                    </ListGroup.Item>
                  ))}
                </ListGroup>
              ) : (
                <Alert variant="secondary" className="mb-0">No coworkings are available for the current subject.</Alert>
              )}
            </CardBody>
          </Card>

          {archivedCoworkings.length > 0 ? (
            <Card className="content-card">
              <CardBody>
                <CardTitle as="h2" className="h4 mb-3">Archived coworkings</CardTitle>
                <ListGroup variant="flush">
                  {archivedCoworkings.map((coworking) => (
                    <ListGroup.Item key={coworking.id} className="px-0 entity-card">
                      <div className="fw-semibold">{coworking.name}</div>
                      <div className="text-body-secondary">ID: {coworking.id}</div>
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
