'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';

interface LoginState {
  email: string;
  password: string;
}

export function LoginForm() {
  const router = useRouter();
  const [formState, setFormState] = useState<LoginState>({ email: '', password: '' });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(formState),
      });
      const data = (await response.json().catch(() => null)) as { message?: string } | null;
      if (!response.ok) {
        throw new Error(data?.message || 'Login failed. Please try again.');
      }
      router.replace('/dashboard');
      router.refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Login failed. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="auth-shell d-flex align-items-center">
      <Container className="py-5">
        <Row className="justify-content-center">
          <Col md={8} lg={5}>
            <Card className="auth-card">
              <Card.Body className="p-4 p-lg-5">
                <Card.Title as="h1" className="mb-2">Admin Login</Card.Title>
                <Card.Text className="text-body-secondary mb-4">Sign in with an administrator account.</Card.Text>
                <Form onSubmit={handleSubmit}>
                  <Form.Group className="mb-3" controlId="email">
                    <Form.Label>Email</Form.Label>
                    <Form.Control
                      autoComplete="email"
                      name="email"
                      onChange={(event) => setFormState((state) => ({ ...state, email: event.target.value }))}
                      placeholder="admin@test.test"
                      required
                      type="email"
                      value={formState.email}
                    />
                  </Form.Group>
                  <Form.Group className="mb-3" controlId="password">
                    <Form.Label>Password</Form.Label>
                    <Form.Control
                      autoComplete="current-password"
                      name="password"
                      onChange={(event) => setFormState((state) => ({ ...state, password: event.target.value }))}
                      placeholder="Enter password"
                      required
                      type="password"
                      value={formState.password}
                    />
                  </Form.Group>
                  {errorMessage ? <Alert variant="danger">{errorMessage}</Alert> : null}
                  <Button className="w-100" disabled={isSubmitting} type="submit">
                    {isSubmitting ? 'Signing in...' : 'Sign in'}
                  </Button>
                </Form>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Container>
    </main>
  );
}
