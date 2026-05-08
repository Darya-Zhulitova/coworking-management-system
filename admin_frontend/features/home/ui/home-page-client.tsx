'use client';

import Link from 'next/link';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import CardBody from 'react-bootstrap/CardBody';
import Container from 'react-bootstrap/Container';
import Stack from 'react-bootstrap/Stack';

export function HomePageClient() {
  return (
    <main className="page-shell d-flex align-items-center">
      <Container className="py-5" style={{ maxWidth: 760 }}>
        <h2 className="mb-4">Администрирование коворкинга</h2>
        <Card className="content-card">
          <CardBody className="p-4 p-lg-5">
            <Stack gap={3}>
              <div>
                <Button as={Link} href="/login">
                  Войти
                </Button>
              </div>
            </Stack>
          </CardBody>
        </Card>
      </Container>
    </main>
  );
}