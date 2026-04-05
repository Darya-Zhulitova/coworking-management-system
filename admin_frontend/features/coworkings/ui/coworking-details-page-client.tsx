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
import { TenantNav } from '@/components/tenant-nav';
import { requestJson } from '@/lib/client/api';
import type { Coworking, CoworkingDashboard } from '@/types/coworking';
import { useAdminSession } from '@/features/session/use-admin-session';

export function CoworkingDetailsPageClient({ coworkingId }: { coworkingId: number }) {
  const { session, isLoading: isSessionLoading, errorMessage: sessionError } = useAdminSession({ redirectToLogin: true });
  const [coworking, setCoworking] = useState<Coworking | null>(null);
  const [grantedActions, setGrantedActions] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      requestJson<Coworking>(`/api/coworkings/${coworkingId}`),
      requestJson<CoworkingDashboard>(`/api/coworkings/${coworkingId}/dashboard`),
    ])
      .then(([coworkingData, dashboardData]) => {
        if (!isMounted) return;
        setCoworking(coworkingData);
        setGrantedActions(dashboardData.grantedActions);
      })
      .catch((error) => {
        if (isMounted) setErrorMessage(error instanceof Error ? error.message : 'Unable to load coworking.');
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [coworkingId]);

  if (isSessionLoading || isLoading) return <FullPageLoader label="Loading coworking details..." />;
  if (sessionError) return <FullPageError message={sessionError} />;
  if (!session) return <FullPageLoader label="Redirecting to login..." />;
  if (errorMessage) return <FullPageError message={errorMessage} />;
  if (!coworking) return <FullPageError message="Coworking not found." />;

  const canEdit = grantedActions.includes('UPDATE_COWORKING');
  const canViewStaff = grantedActions.includes('VIEW_STAFF_ACCESS');

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5">
        <Stack gap={4}>
          <TenantNav coworkingId={coworkingId} />
          <Card className="content-card">
            <CardBody>
              <CardTitle as="h1" className="mb-3">{coworking.name}</CardTitle>
              <Stack direction="horizontal" gap={2} className="flex-wrap mb-3">
                <Badge bg="secondary">Coworking ID: {coworking.id}</Badge>
                <Badge bg={coworking.active ? 'success' : 'secondary'}>{coworking.active ? 'Active' : 'Inactive'}</Badge>
                <Badge bg={coworking.archived ? 'dark' : 'info'}>{coworking.archived ? 'Archived' : 'Visible'}</Badge>
                <Badge bg={canEdit ? 'primary' : 'light'} text={canEdit ? undefined : 'dark'}>{canEdit ? 'Editable' : 'Read only'}</Badge>
                {canViewStaff ? <Badge bg="dark">Staff access visible</Badge> : null}
              </Stack>
            </CardBody>
          </Card>
          {canEdit ? (
            <Card className="content-card">
              <CardBody>
                <CardTitle as="h2" className="h4 mb-3">Edit coworking</CardTitle>
                <CoworkingEditForm coworking={coworking} />
              </CardBody>
            </Card>
          ) : (
            <Alert variant="secondary" className="mb-0">This subject can view coworking details but cannot edit tenant configuration.</Alert>
          )}
        </Stack>
      </Container>
    </main>
  );
}
