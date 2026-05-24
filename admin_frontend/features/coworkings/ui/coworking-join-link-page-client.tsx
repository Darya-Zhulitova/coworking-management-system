'use client';

import { useEffect, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import InputGroup from 'react-bootstrap/InputGroup';
import Stack from 'react-bootstrap/Stack';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import type { CoworkingJoinLink } from '@/types/coworking';

export function CoworkingJoinLinkPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true,
  });
  const [joinLink, setJoinLink] = useState<CoworkingJoinLink | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    requestJson<CoworkingJoinLink>(`/api/coworkings/${coworkingId}/join-link`)
      .then((data) => {
        if (isMounted) setJoinLink(data);
      })
      .catch((error) => {
        if (isMounted) setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить ссылку.');
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });
    return () => {
      isMounted = false;
    };
  }, [coworkingId]);

  async function updateLink(method: 'POST' | 'DELETE', message: string) {
    setIsSaving(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const data = await requestJson<CoworkingJoinLink>(`/api/coworkings/${coworkingId}/join-link`, { method });
      setJoinLink(data);
      setSuccessMessage(message);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось обновить ссылку.');
    } finally {
      setIsSaving(false);
    }
  }

  async function copyLink() {
    if (!joinLink?.joinUrl) return;
    try {
      await navigator.clipboard.writeText(joinLink.joinUrl);
      setSuccessMessage('Ссылка скопирована в буфер обмена.');
    } catch {
      setErrorMessage('Не удалось скопировать ссылку автоматически.');
    }
  }

  if (isContextLoading || isLoading) return <FullPageLoader label="Загрузка ссылки на коворкинг..."/>;
  if (contextError) return <FullPageError message={contextError}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;
  if (!context.grants.includes('COWORKING_READ')) return <FullPageError message="Нет доступа к ссылке коворкинга."/>;

  const canEdit = context.grants.includes('COWORKING_EDIT');
  const hasLink = Boolean(joinLink?.joinUrl);

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5">
        <Stack gap={4}>
          <div>
            <h2 className="mb-2">Ссылка на коворкинг</h2>
            <p className="text-body-secondary mb-0">
              По этой ссылке пользователь открывает краткую информацию о коворкинге и отправляет запрос на
              присоединение.
            </p>
          </div>

          {errorMessage ?
            <Alert variant="danger" onClose={() => setErrorMessage(null)} dismissible>{errorMessage}</Alert> : null}
          {successMessage ? <Alert variant="success" onClose={() => setSuccessMessage(null)}
                                   dismissible>{successMessage}</Alert> : null}

          <Card className="content-card">
            <Card.Body className="d-grid gap-4">
              <div>
                <Card.Title as="h3" className="h5 mb-2">Текущая ссылка</Card.Title>
                <Card.Text className="text-body-secondary mb-0">
                  Перегенерация создает новый UUID и сразу делает старую ссылку недействительной. Удаление очищает поле
                  ссылки в БД.
                </Card.Text>
              </div>

              {hasLink ? (
                <InputGroup>
                  <Form.Control value={joinLink?.joinUrl ?? ''} readOnly
                                aria-label="Ссылка для присоединения к коворкингу"/>
                  <Button variant="outline-secondary" onClick={copyLink}>Копировать</Button>
                </InputGroup>
              ) : (
                <Alert variant="secondary" className="mb-0">
                  Ссылка еще не создана. Пользователи не смогут присоединиться к коворкингу по приглашению, пока вы ее
                  не сгенерируете.
                </Alert>
              )}

              <Stack direction="horizontal" gap={2} className="flex-wrap">
                <Button
                  onClick={() => updateLink('POST', hasLink ? 'Ссылка перегенерирована.' : 'Ссылка сгенерирована.')}
                  disabled={!canEdit || isSaving}>
                  {hasLink ? 'Перегенерировать' : 'Сгенерировать'}
                </Button>
                <Button variant="outline-danger" onClick={() => updateLink('DELETE', 'Ссылка удалена.')}
                        disabled={!canEdit || isSaving || !hasLink}>
                  Удалить ссылку
                </Button>
              </Stack>

              {!canEdit ? (
                <Alert variant="secondary" className="mb-0">
                  У этой учетной записи есть право просмотра, но нет права изменять ссылку.
                </Alert>
              ) : null}
            </Card.Body>
          </Card>
        </Stack>
      </Container>
    </main>
  );
}
