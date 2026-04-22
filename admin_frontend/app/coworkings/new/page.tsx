'use client';

import Link from 'next/link';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Stack from 'react-bootstrap/Stack';
import { CoworkingCreateForm } from '@/components/coworking-create-form';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';

export default function CoworkingCreatePage() {
  const { context, isLoading, errorMessage } = useAppContext({ redirectToLogin: true });

  if (isLoading) {
    return <FullPageLoader label="Загрузка формы создания коворкинга..."/>;
  }

  if (errorMessage) {
    return <FullPageError message={errorMessage}/>;
  }

  if (!context) {
    return <FullPageLoader label="Переход на страницу входа..."/>;
  }

  const canCreate = context.grants.includes('COWORKING_EDIT');

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5" style={{ maxWidth: 840 }}>
        <Stack gap={4}>
          <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
            <div>
              <h2 className="mb-2">Создать коворкинг</h2>
            </div>
            <Button as={Link} href="/coworkings" variant="outline-secondary">Назад к списку</Button>
          </div>

          {!canCreate ? (
            <Alert variant="secondary" className="mb-0">У вас нет прав на создание коворкинга.</Alert>
          ) : (
            <Card className="content-card">
              <Card.Body>
                <CoworkingCreateForm/>
              </Card.Body>
            </Card>
          )}
        </Stack>
      </Container>
    </main>
  );
}
