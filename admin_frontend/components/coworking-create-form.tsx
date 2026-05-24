'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';

export function CoworkingCreateForm() {
  const router = useRouter();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [address, setAddress] = useState('');
  const [workingHoursLabel, setWorkingHoursLabel] = useState('');
  const [heroTitle, setHeroTitle] = useState('');
  const [heroText, setHeroText] = useState('');
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [autoApproveMembership, setAutoApproveMembership] = useState(false);
  const [floorMapEnabled, setFloorMapEnabled] = useState(false);
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
        body: JSON.stringify({
          name,
          description,
          address,
          workingHoursLabel,
          heroTitle,
          heroText,
          autoApproveMembership,
          floorMapEnabled
        })
      });
      const data = await response.json().catch(() => null) as { id?: number; message?: string } | null;
      if (!response.ok || !data?.id) throw new Error(data?.message || 'Не удалось создать коворкинг.');
      await uploadSelectedPhotos(data.id);
      router.push(`/coworkings/${data.id}/users`);
      router.refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось создать коворкинг.');
    } finally {
      setIsSubmitting(false);
    }
  }

  async function uploadSelectedPhotos(coworkingId: number) {
    for (const file of imageFiles) {
      const formData = new FormData();
      formData.set('file', file);
      const response = await fetch(`/api/coworkings/${coworkingId}/photos`, {
        method: 'POST',
        body: formData,
        headers: { Accept: 'application/json' }
      });
      const data = await response.json().catch(() => null) as { message?: string } | null;
      if (!response.ok) {
        throw new Error(data?.message || `Не удалось загрузить фото «${file.name}».`);
      }
    }
  }

  return (
    <Form onSubmit={handleSubmit}>
      <Form.Group className="mb-3" controlId="createCoworkingName"><Form.Label>Название</Form.Label><Form.Control
        value={name} onChange={(e) => setName(e.target.value)} required placeholder="Новый коворкинг"/></Form.Group>
      <Form.Group className="mb-3" controlId="createCoworkingDescription"><Form.Label>Описание</Form.Label><Form.Control
        as="textarea" rows={4} value={description} onChange={(e) => setDescription(e.target.value)} required
        placeholder="Опишите коворкинг"/></Form.Group>
      <Form.Group className="mb-3" controlId="createCoworkingAddress"><Form.Label>Адрес</Form.Label><Form.Control
        value={address} onChange={(e) => setAddress(e.target.value)} required
        placeholder="Город, улица, дом"/></Form.Group>
      <Form.Group className="mb-3"
                  controlId="createCoworkingWorkingHoursLabel"><Form.Label>Расписание</Form.Label><Form.Control
        value={workingHoursLabel} onChange={(e) => setWorkingHoursLabel(e.target.value)} required
        placeholder="Пн–Вс, 08:00–22:00"/></Form.Group>
      <Form.Group className="mb-3" controlId="createCoworkingHeroTitle"><Form.Label>Заголовок
        баннера</Form.Label><Form.Control value={heroTitle} onChange={(e) => setHeroTitle(e.target.value)}
                                          placeholder="Необязательный заголовок"/></Form.Group>
      <Form.Group className="mb-3" controlId="createCoworkingHeroText"><Form.Label>Текст
        баннера</Form.Label><Form.Control as="textarea" rows={3} value={heroText}
                                          onChange={(e) => setHeroText(e.target.value)}
                                          placeholder="Необязательный текст"/></Form.Group>
      <Form.Group className="mb-3" controlId="createCoworkingImageFiles">
        <Form.Label>Загрузить фотографии</Form.Label>
        <Form.Control
          type="file"
          accept="image/jpeg,image/png,image/webp"
          multiple
          onChange={(event) => setImageFiles(Array.from(event.currentTarget.files ?? []))}
        />
        <Form.Text className="text-body-secondary">
          JPG, PNG или WEBP, до 10 МБ. Изображения будут обрезаны до формата 16:9 и сохранены как JPEG.
        </Form.Text>
      </Form.Group>
      <Form.Check className="mb-3" type="switch" id="createCoworkingAutoApproveMembership"
                  checked={autoApproveMembership} onChange={(e) => setAutoApproveMembership(e.target.checked)}
                  label="Не требовать подтверждения для новых участников"/>
      <Form.Check className="mb-3" type="switch" id="createCoworkingFloorMapEnabled"
                  checked={floorMapEnabled} onChange={(e) => setFloorMapEnabled(e.target.checked)}
                  label="Использовать карты этажей для бронирования"/>
      {errorMessage ? <Alert variant="danger">{errorMessage}</Alert> : null}
      <Button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Создание...' : 'Создать коворкинг'}</Button>
    </Form>
  );
}
