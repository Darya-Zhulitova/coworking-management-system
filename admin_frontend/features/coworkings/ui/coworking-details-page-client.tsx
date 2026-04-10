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
import { CoworkingNav } from '@/components/coworking-nav';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import type { Coworking } from '@/types/coworking';

export function CoworkingDetailsPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({ coworkingId, redirectToLogin: true });
  const [coworking, setCoworking] = useState<Coworking | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    requestJson<Coworking>(`/api/coworkings/${coworkingId}`)
      .then((coworkingData) => {
        if (!isMounted) return;
        setCoworking(coworkingData);
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

  if (isContextLoading || isLoading) return <FullPageLoader label="Loading coworking details..." />;
  if (contextError) return <FullPageError message={contextError} />;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Redirecting to login..." />;
  if (errorMessage) return <FullPageError message={errorMessage} />;
  if (!coworking) return <FullPageError message="Coworking not found." />;

  const canEdit = context.grants.includes('COWORKING_EDIT');
  const canViewAccess = context.grants.includes('ACCESS_VIEW');

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5">
        <Stack gap={4}>
          <CoworkingNav coworkingId={coworkingId} grants={context.grants} />
          <Card className="content-card">
            <CardBody>
              <CardTitle as="h1" className="mb-3">{coworking.name}</CardTitle>
              <Stack direction="horizontal" gap={2} className="flex-wrap mb-3">
                <Badge bg="secondary">Coworking ID: {coworking.id}</Badge>
                <Badge bg={coworking.active ? 'success' : 'secondary'}>{coworking.active ? 'Active' : 'Inactive'}</Badge>
                <Badge bg={coworking.archived ? 'dark' : 'info'}>{coworking.archived ? 'Archived' : 'Visible'}</Badge>
                <Badge bg="info">{context.role}</Badge>
                <Badge bg={canEdit ? 'primary' : 'light'} text={canEdit ? undefined : 'dark'}>{canEdit ? 'Editable' : 'Read only'}</Badge>
                {canViewAccess ? <Badge bg="dark">Access visible</Badge> : null}
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
            <Alert variant="secondary" className="mb-0">This subject can view coworking details but cannot edit coworking configuration.</Alert>
          )}
        </Stack>
      </Container>
    </main>
  );
}
