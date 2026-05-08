'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Stack from 'react-bootstrap/Stack';
import type { Coworking } from '@/types/coworking';
import { CoworkingImageLinksEditor } from '@/components/coworking-image-links-editor';

export function CoworkingEditForm({ coworking }: { coworking: Coworking }) {
  const router = useRouter();
  const [name, setName] = useState(coworking.name);
  const [description, setDescription] = useState(coworking.description);
  const [address, setAddress] = useState(coworking.address);
  const [workingHoursLabel, setWorkingHoursLabel] = useState(coworking.workingHoursLabel);
  const [heroTitle, setHeroTitle] = useState(coworking.heroTitle ?? '');
  const [heroText, setHeroText] = useState(coworking.heroText ?? '');
  const [imageUrls, setImageUrls] = useState<string[]>(coworking.imageUrls ?? []);
  const [active, setActive] = useState(coworking.active);
  const [autoApproveMembership, setAutoApproveMembership] = useState(coworking.autoApproveMembership);
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
        body: JSON.stringify({
          name,
          description,
          address,
          workingHoursLabel,
          heroTitle,
          heroText,
          imageUrls,
          active,
          autoApproveMembership
        })
      });
      const data = await response.json().catch(() => null) as { message?: string } | null;
      if (!response.ok) throw new Error(data?.message || 'Не удалось обновить коворкинг.');
      router.push(`/coworkings/${coworking.id}/users`);
      router.refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось обновить коворкинг.');
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleArchive() {
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      const response = await fetch(`/api/coworkings/${coworking.id}`, {
        method: 'DELETE',
        headers: { Accept: 'application/json' }
      });
      const data = await response.json().catch(() => null) as { message?: string } | null;
      if (!response.ok) throw new Error(data?.message || 'Не удалось архивировать коворкинг.');
      router.push('/coworkings');
      router.refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось архивировать коворкинг.');
      setIsSubmitting(false);
    }
  }

  return (
    <Form onSubmit={handleSubmit}>
      <Form.Group className="mb-3" controlId="editCoworkingName"><Form.Label>Название</Form.Label><Form.Control
        value={name} onChange={(e) => setName(e.target.value)} required/></Form.Group>
      <Form.Group className="mb-3" controlId="editCoworkingDescription"><Form.Label>Описание</Form.Label><Form.Control
        as="textarea" rows={4} value={description} onChange={(e) => setDescription(e.target.value)}
        required/></Form.Group>
      <Form.Group className="mb-3" controlId="editCoworkingAddress"><Form.Label>Адрес</Form.Label><Form.Control
        value={address} onChange={(e) => setAddress(e.target.value)} required/></Form.Group>
      <Form.Group className="mb-3" controlId="editCoworkingWorkingHoursLabel"><Form.Label>Часы
        работы</Form.Label><Form.Control value={workingHoursLabel}
                                         onChange={(e) => setWorkingHoursLabel(e.target.value)} required/></Form.Group>
      <Form.Group className="mb-3" controlId="editCoworkingHeroTitle"><Form.Label>Заголовок
        баннера</Form.Label><Form.Control value={heroTitle} onChange={(e) => setHeroTitle(e.target.value)}
                                          placeholder="Необязательный заголовок"/></Form.Group>
      <Form.Group className="mb-3" controlId="editCoworkingHeroText"><Form.Label>Текст баннера</Form.Label><Form.Control
        as="textarea" rows={3} value={heroText} onChange={(e) => setHeroText(e.target.value)}
        placeholder="Необязательный текст"/></Form.Group>
      <div className="mb-3"><CoworkingImageLinksEditor imageUrls={imageUrls} onChange={setImageUrls}
                                                       controlIdPrefix="editCoworkingImageUrl"/></div>
      <Form.Check className="mb-3" type="switch" id="editCoworkingActive" checked={active}
                  onChange={(e) => setActive(e.target.checked)} label="Коворкинг активен"/>
      <Form.Check className="mb-3" type="switch" id="editCoworkingAutoApproveMembership" checked={autoApproveMembership}
                  onChange={(e) => setAutoApproveMembership(e.target.checked)} label="Автоподтверждение участия"/>
      {errorMessage ? <Alert variant="danger">{errorMessage}</Alert> : null}
      <Stack direction="horizontal" gap={2} className="flex-wrap"><Button type="submit"
                                                                          disabled={isSubmitting}>{isSubmitting ? 'Сохранение...' : 'Сохранить изменения'}</Button><Button
        variant="outline-danger" type="button" onClick={handleArchive} disabled={isSubmitting}>Архивировать
        коворкинг</Button></Stack>
    </Form>
  );
}
