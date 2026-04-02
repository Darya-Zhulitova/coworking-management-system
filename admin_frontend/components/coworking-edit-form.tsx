'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Stack from 'react-bootstrap/Stack';
import type { Coworking } from '@/types/coworking';

export function CoworkingEditForm({ coworking }: { coworking: Coworking }) {
  const router = useRouter();
  const [name, setName] = useState(coworking.name);
  const [active, setActive] = useState(coworking.active);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      const response = await fetch(`/api/coworkings/${coworking.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ name, active }),
      });
      const data = await response.json().catch(() => null) as { message?: string } | null;
      if (!response.ok) throw new Error(data?.message || 'Unable to update coworking.');
      router.refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to update coworking.');
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleArchive() {
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      const response = await fetch(`/api/coworkings/${coworking.id}`, { method: 'DELETE', headers: { Accept: 'application/json' } });
      const data = await response.json().catch(() => null) as { message?: string } | null;
      if (!response.ok) throw new Error(data?.message || 'Unable to archive coworking.');
      router.push('/coworkings');
      router.refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to archive coworking.');
      setIsSubmitting(false);
    }
  }

  return (
    <Form onSubmit={handleSubmit}>
      <Form.Group className="mb-3" controlId="editCoworkingName">
        <Form.Label>Name</Form.Label>
        <Form.Control value={name} onChange={(e) => setName(e.target.value)} required />
      </Form.Group>
      <Form.Check className="mb-3" type="switch" id="editCoworkingActive" checked={active} onChange={(e) => setActive(e.target.checked)} label="Active coworking" />
      {errorMessage ? <Alert variant="danger">{errorMessage}</Alert> : null}
      <Stack direction="horizontal" gap={2} className="flex-wrap">
        <Button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Saving...' : 'Save changes'}</Button>
        <Button variant="outline-danger" type="button" onClick={handleArchive} disabled={isSubmitting}>Archive coworking</Button>
      </Stack>
    </Form>
  );
}
