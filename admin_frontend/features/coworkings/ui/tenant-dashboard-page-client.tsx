'use client';

import { useEffect, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Card from 'react-bootstrap/Card';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import ListGroup from 'react-bootstrap/ListGroup';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import { LogoutButton } from '@/components/logout-button';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { TenantNav } from '@/components/tenant-nav';
import { requestJson } from '@/lib/client/api';
import type { CoworkingDashboard } from '@/types/coworking';
import { useAdminSession } from '@/features/session/use-admin-session';

export function TenantDashboardPageClient({ coworkingId }: { coworkingId: number }) {
  const { session, isLoading: isSessionLoading, errorMessage: sessionError } = useAdminSession({ redirectToLogin: true });
  const [dashboard, setDashboard] = useState<CoworkingDashboard | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    requestJson<CoworkingDashboard>(`/api/coworkings/${coworkingId}/dashboard`)
      .then((data) => {
        if (isMounted) setDashboard(data);
      })
      .catch((error) => {
        if (isMounted) setErrorMessage(error instanceof Error ? error.message : 'Unable to load dashboard.');
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [coworkingId]);

  if (isSessionLoading || isLoading) return <FullPageLoader label="Loading tenant dashboard..." />;
  if (sessionError) return <FullPageError message={sessionError} />;
  if (!session) return <FullPageLoader label="Redirecting to login..." />;
  if (errorMessage) return <FullPageError message={errorMessage} />;
  if (!dashboard) return <FullPageError message="Dashboard is unavailable." />;

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5">
        <Stack gap={4}>
          <TenantNav coworkingId={coworkingId} />
          <Card className="content-card">
            <Card.Body>
              <Stack direction="horizontal" className="justify-content-between align-items-start gap-3 flex-wrap mb-4">
                <div>
                  <Card.Title as="h1" className="mb-2">{dashboard.coworking.name} dashboard</Card.Title>
                  <Card.Text className="mb-1">Tenant ID: {dashboard.coworking.id}</Card.Text>
                  <Card.Text className="mb-0 text-body-secondary">Resolved subject: {dashboard.subjectLabel}</Card.Text>
                </div>
                <LogoutButton />
              </Stack>

              <Row className="g-3 mb-4">
                <Col md={6}>
                  <Card bg="light">
                    <Card.Body>
                      <Card.Title as="h2" className="h5">Tenant status</Card.Title>
                      <Stack direction="horizontal" gap={2} className="flex-wrap">
                        <Badge bg={dashboard.coworking.active ? 'success' : 'secondary'}>{dashboard.coworking.active ? 'Active' : 'Inactive'}</Badge>
                        <Badge bg={dashboard.coworking.archived ? 'dark' : 'info'}>{dashboard.coworking.archived ? 'Archived' : 'Visible'}</Badge>
                      </Stack>
                    </Card.Body>
                  </Card>
                </Col>
              </Row>

              <Card.Title as="h2" className="h4 mb-3">Granted tenant actions</Card.Title>
              {dashboard.grantedActions.length > 0 ? (
                <ListGroup>
                  {dashboard.grantedActions.map((action) => (
                    <ListGroup.Item key={action}>{action}</ListGroup.Item>
                  ))}
                </ListGroup>
              ) : (
                <Alert variant="secondary" className="mb-0">No tenant actions resolved for this subject.</Alert>
              )}
            </Card.Body>
          </Card>
        </Stack>
      </Container>
    </main>
  );
}
