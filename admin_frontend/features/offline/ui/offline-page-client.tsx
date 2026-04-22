'use client';

import Link from 'next/link';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import CardBody from 'react-bootstrap/CardBody';
import CardText from 'react-bootstrap/CardText';
import Container from 'react-bootstrap/Container';
import Stack from 'react-bootstrap/Stack';

export function OfflinePageClient() {
  return (
    <main className="page-shell d-flex align-items-center">
      <Container className="py-5" style={{ maxWidth: 760 }}>
        <h2 className="mb-4">Нет подключения</h2>
        <Card className="content-card">
          <CardBody className="p-4 p-lg-5">
            <Stack gap={3}>
              <div>
                <CardText className="mb-0 text-body-secondary">Сервер сейчас недоступен. Проверьте подключение и
                  повторите попытку.</CardText>
              </div>

              <div className="d-flex gap-2 flex-wrap">
                <Button onClick={() => window.location.reload()}>
                  Повторить
                </Button>
                <Button as={Link} href="/login" variant="outline-secondary">
                  К странице входа
                </Button>
              </div>
            </Stack>
          </CardBody>
        </Card>
      </Container>
    </main>
  );
}