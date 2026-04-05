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
import { useAdminSession } from '@/features/session/use-admin-session';

function formatAccessLabel(isOwner: boolean, role: string | null) {
  if (isOwner) return 'Owner';
  if (role === 'MANAGER') return 'Manager';
  if (role === 'STAFF_SUPPORT') return 'Staff support';
  return 'Assigned';
}

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
            <CardBody>
              <Stack direction="horizontal" className="justify-content-between align-items-start gap-3 flex-wrap">
                <div>
                  <CardTitle as="h1" className="mb-2">Coworkings</CardTitle>
                  <CardText className="mb-1">Principal type: <strong>{session.principalType}</strong></CardText>
                  {session.principalType === 'TENANT_ADMIN' ? (
                    <CardText className="mb-0 text-body-secondary">Access level is resolved per tenant from owner status or assigned role.</CardText>
                  ) : null}
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
              <CardTitle as="h2" className="h4 mb-3">{canViewAll ? 'All active coworkings' : 'Accessible coworkings'}</CardTitle>
              {errorMessage ? <Alert variant="danger">{errorMessage}</Alert> : null}
              {coworkings.length > 0 ? (
                <ListGroup variant="flush">
                  {coworkings.map((coworking) => {
                    const access = session.coworkings.find((item) => item.id === coworking.id);
                    return (
                      <ListGroup.Item key={coworking.id} className="px-0 entity-card">
                        <Row className="g-3 align-items-center">
                          <Col md>
                            <div className="fw-semibold fs-5">{coworking.name}</div>
                            <div className="text-body-secondary">ID: {coworking.id}</div>
                            <Stack direction="horizontal" gap={2} className="flex-wrap mt-2">
                              <Badge bg={coworking.active ? 'success' : 'secondary'}>{coworking.active ? 'Active' : 'Inactive'}</Badge>
                              {access ? (
                                <Badge bg={access.owner ? 'dark' : 'info'}>{formatAccessLabel(access.owner, access.role)}</Badge>
                              ) : null}
                            </Stack>
                          </Col>
                          <Col md="auto">
                            <Stack direction="horizontal" gap={2} className="flex-wrap">
                              <Button as={Link} href={`/coworkings/${coworking.id}/dashboard`} variant="primary">Open dashboard</Button>
                              <Button as={Link} href={`/coworkings/${coworking.id}`} variant="outline-primary">Details</Button>
                              <Button as={Link} href={`/coworkings/${coworking.id}/staff`} variant="outline-secondary">Staff</Button>
                            </Stack>
                          </Col>
                        </Row>
                      </ListGroup.Item>
                    );
                  })}
                </ListGroup>
              ) : (
                <Alert variant="secondary" className="mb-0">No coworkings are available for the current subject.</Alert>
              )}
            </CardBody>
          </Card>

          {session.principalType === 'SUPERADMIN' ? (
            <Card className="content-card">
              <CardBody>
                <CardTitle as="h2" className="h4 mb-3">Archived coworkings</CardTitle>
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
              </CardBody>
            </Card>
          ) : null}
        </Stack>
      </Container>
    </main>
  );
}
