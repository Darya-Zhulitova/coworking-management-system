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
import { CoworkingNav } from '@/components/coworking-nav';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import type { CoworkingDashboard } from '@/types/coworking';

export function CoworkingDashboardPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({ coworkingId, redirectToLogin: true });
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

  if (isContextLoading || isLoading) return <FullPageLoader label="Loading coworking dashboard..." />;
  if (contextError) return <FullPageError message={contextError} />;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Redirecting to login..." />;
  if (errorMessage) return <FullPageError message={errorMessage} />;
  if (!dashboard) return <FullPageError message="Dashboard is unavailable." />;

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5">
        <Stack gap={4}>
          <CoworkingNav coworkingId={coworkingId} grants={context.grants} />
          <Card className="content-card">
            <Card.Body>
              <Stack direction="horizontal" className="justify-content-between align-items-start gap-3 flex-wrap mb-4">
                <div>
                  <Card.Title as="h1" className="mb-2">{context.coworkingName} dashboard</Card.Title>
                  <Card.Text className="mb-1">Coworking ID: {context.coworkingId}</Card.Text>
                  <Card.Text className="mb-0 text-body-secondary">Resolved access: {context.role}</Card.Text>
                </div>
                <LogoutButton />
              </Stack>

              <Row className="g-3 mb-4">
                <Col md={6}>
                  <Card bg="light">
                    <Card.Body>
                      <Card.Title as="h2" className="h5">Coworking status</Card.Title>
                      <Stack direction="horizontal" gap={2} className="flex-wrap">
                        <Badge bg={dashboard.coworking.active ? 'success' : 'secondary'}>{dashboard.coworking.active ? 'Active' : 'Inactive'}</Badge>
                        <Badge bg={dashboard.coworking.archived ? 'dark' : 'info'}>{dashboard.coworking.archived ? 'Archived' : 'Visible'}</Badge>
                      </Stack>
                    </Card.Body>
                  </Card>
                </Col>
              </Row>

              <Card.Title as="h2" className="h4 mb-3">Resolved grants</Card.Title>
              {context.grants.length > 0 ? (
                <ListGroup variant="flush">
                  {context.grants.map((grant) => (
                    <ListGroup.Item key={grant} className="px-0">{grant}</ListGroup.Item>
                  ))}
                </ListGroup>
              ) : (
                <Alert variant="secondary" className="mb-0">No grants resolved for this subject.</Alert>
              )}
            </Card.Body>
          </Card>
        </Stack>
      </Container>
    </main>
  );
}
