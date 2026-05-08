'use client';

import Alert from 'react-bootstrap/Alert';
import Container from 'react-bootstrap/Container';
import Spinner from 'react-bootstrap/Spinner';

export function FullPageLoader({ label = 'Загрузка...' }: { label?: string }) {
  return (
    <main className="page-shell d-flex align-items-center">
      <Container className="py-5 text-center">
        <Spinner animation="border" role="status" className="mb-3"/>
        <div>{label}</div>
      </Container>
    </main>
  );
}

export function FullPageError({ message }: { message: string }) {
  return (
    <main className="page-shell d-flex align-items-center">
      <Container className="py-5" style={{ maxWidth: 720 }}>
        <Alert variant="danger" className="mb-0">{message}</Alert>
      </Container>
    </main>
  );
}
