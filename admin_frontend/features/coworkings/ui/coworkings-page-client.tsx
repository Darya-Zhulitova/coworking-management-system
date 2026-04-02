'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
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
import { useAdminSession } from '@/features/session/use-admin-session';

export function CoworkingsPageClient() {
  const { session, isLoading: isSessionLoading, errorMessage: sessionError } = useAdminSession({ redirectToLogin: true });
  const [coworkings, setCoworkings] = useState<Coworking[]>([]);
  const [archivedCoworkings, setArchivedCoworkings] = useState<Coworking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const canCreate = useMemo(() => session?.grantedGlobalActions.includes('CREATE_COWORKING') ?? false, [session]);
  const canViewAll = useMemo(() => session?.grantedGlobalActions.includes('VIEW_ALL_COWORKINGS') ?? false, [session]);

  useEffect(() => {
    if (!session) return;
    let isMounted = true;

    Promise.all([
      requestJson<Coworking[]>('/api/coworkings'),
      session.principalType === 'SUPERADMIN' ? requestJson<Coworking[]>('/api/coworkings?archived=true') : Promise.resolve([]),
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
  }, [session]);

  if (isSessionLoading || (session && isLoading)) {
    return <FullPageLoader label="Loading coworkings..." />;
  }
  if (sessionError) {
    return <FullPageError message={sessionError} />;
  }
  if (!session) {
    return <FullPageLoader label="Redirecting to login..." />;
  }

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5">
        <Stack gap={4}>
          <Card className="content-card">
            <Card.Body>
              <Stack direction="horizontal" className="justify-content-between align-items-start gap-3 flex-wrap">
                <div>
                  <Card.Title as="h1" className="mb-2">Coworkings</Card.Title>
                  <Card.Text className="mb-1">Principal type: <strong>{session.principalType}</strong></Card.Text>
                </div>
                <LogoutButton />
              </Stack>
            </Card.Body>
          </Card>

          {canCreate ? (
            <Card className="content-card">
              <Card.Body>
                <Card.Title as="h2" className="h4 mb-3">Create coworking</Card.Title>
                <CoworkingCreateForm />
              </Card.Body>
            </Card>
          ) : null}

          <Card className="content-card">
            <Card.Body>
              <Card.Title as="h2" className="h4 mb-3">{canViewAll ? 'All active coworkings' : 'Accessible coworkings'}</Card.Title>
              {errorMessage ? <Alert variant="danger">{errorMessage}</Alert> : null}
              {coworkings.length > 0 ? (
                <ListGroup variant="flush">
                  {coworkings.map((coworking) => (
                    <ListGroup.Item key={coworking.id} className="px-0 entity-card">
                      <Row className="g-3 align-items-center">
                        <Col md>
                          <div className="fw-semibold fs-5">{coworking.name}</div>
                          <div className="text-body-secondary">ID: {coworking.id}</div>
                          <Badge bg={coworking.active ? 'success' : 'secondary'} className="mt-2">{coworking.active ? 'Active' : 'Inactive'}</Badge>
                        </Col>
                        <Col md="auto">
                          <Stack direction="horizontal" gap={2} className="flex-wrap">
                            <Button as={Link} href={`/coworkings/${coworking.id}/dashboard`} variant="primary">Open dashboard</Button>
                            <Button as={Link} href={`/coworkings/${coworking.id}`} variant="outline-primary">Details</Button>
                          </Stack>
                        </Col>
                      </Row>
                    </ListGroup.Item>
                  ))}
                </ListGroup>
              ) : (
                <Alert variant="secondary" className="mb-0">No coworkings are available for the current subject.</Alert>
              )}
            </Card.Body>
          </Card>

          {session.principalType === 'SUPERADMIN' ? (
            <Card className="content-card">
              <Card.Body>
                <Card.Title as="h2" className="h4 mb-3">Archived coworkings</Card.Title>
                {archivedCoworkings.length > 0 ? (
                  <ListGroup variant="flush">
                    {archivedCoworkings.map((coworking) => (
                      <ListGroup.Item key={coworking.id} className="px-0 entity-card">
                        <div className="fw-semibold">{coworking.name}</div>
                        <div className="text-body-secondary">Archived at: {coworking.archivedAt ?? 'n/a'}</div>
                      </ListGroup.Item>
                    ))}
                  </ListGroup>
                ) : (
                  <Alert variant="secondary" className="mb-0">No archived coworkings yet.</Alert>
                )}
              </Card.Body>
            </Card>
          ) : null}
        </Stack>
      </Container>
    </main>
  );
}
