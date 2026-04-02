'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';

export function CoworkingCreateForm() {
  const router = useRouter();
  const [name, setName] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      const response = await fetch('/api/coworkings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ name }),
      });
      const data = await response.json().catch(() => null) as { id?: number; message?: string } | null;
      if (!response.ok || !data?.id) throw new Error(data?.message || 'Unable to create coworking.');
      router.push(`/coworkings/${data.id}/dashboard`);
      router.refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to create coworking.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Form onSubmit={handleSubmit}>
      <Form.Group className="mb-3" controlId="createCoworkingName">
        <Form.Label>Name</Form.Label>
        <Form.Control value={name} onChange={(e) => setName(e.target.value)} required placeholder="New coworking" />
      </Form.Group>
      {errorMessage ? <Alert variant="danger">{errorMessage}</Alert> : null}
      <Button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Creating...' : 'Create coworking'}</Button>
    </Form>
  );
}
