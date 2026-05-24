'use client';

import { ChangeEvent, FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Stack from 'react-bootstrap/Stack';
import type { Coworking } from '@/types/coworking';

export function CoworkingEditForm({ coworking }: { coworking: Coworking }) {
  const router = useRouter();
  const [name, setName] = useState(coworking.name);
  const [description, setDescription] = useState(coworking.description);
  const [address, setAddress] = useState(coworking.address);
  const [workingHoursLabel, setWorkingHoursLabel] = useState(coworking.workingHoursLabel);
  const [heroTitle, setHeroTitle] = useState(coworking.heroTitle ?? '');
  const [heroText, setHeroText] = useState(coworking.heroText ?? '');
  const [uploadedImages, setUploadedImages] = useState(() => {
    const urls = coworking.uploadedImageUrls ?? [];
    const fileIds = coworking.uploadedImageFileIds ?? [];
    return urls
      .map((url, index) => ({ url, fileId: fileIds[index] }))
      .filter((item): item is { url: string; fileId: string } => Boolean(item.fileId));
  });
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [imageInputKey, setImageInputKey] = useState(0);
  const [active, setActive] = useState(coworking.active);
  const [autoApproveMembership, setAutoApproveMembership] = useState(coworking.autoApproveMembership);
  const [floorMapEnabled, setFloorMapEnabled] = useState(coworking.floorMapEnabled);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isUploadingPhotos, setIsUploadingPhotos] = useState(false);
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
          active,
          autoApproveMembership,
          floorMapEnabled,
          imageFileIds: uploadedImages.map((image) => image.fileId)
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

  function extractUploadedImages(nextCoworking: Coworking) {
    const urls = nextCoworking.uploadedImageUrls ?? [];
    const fileIds = nextCoworking.uploadedImageFileIds ?? [];
    return urls
      .map((url, index) => ({ url, fileId: fileIds[index] }))
      .filter((item): item is { url: string; fileId: string } => Boolean(item.fileId));
  }

  function handleImageFilesChange(event: ChangeEvent<HTMLInputElement>) {
    setImageFiles(Array.from(event.currentTarget.files ?? []));
    setErrorMessage(null);
  }

  async function uploadSelectedPhotos() {
    if (imageFiles.length === 0 || isUploadingPhotos) {
      return;
    }

    setIsUploadingPhotos(true);
    setErrorMessage(null);
    try {
      let latestCoworking: Coworking | null = null;
      for (const file of imageFiles) {
        const formData = new FormData();
        formData.set('file', file);
        const response = await fetch(`/api/coworkings/${coworking.id}/photos`, {
          method: 'POST',
          body: formData,
          headers: { Accept: 'application/json' }
        });
        const data = await response.json().catch(() => null) as (Coworking & { message?: string }) | null;
        if (!response.ok || !data) {
          throw new Error(data?.message || `Не удалось загрузить фото «${file.name}».`);
        }
        latestCoworking = data;
      }
      if (latestCoworking) {
        setUploadedImages(extractUploadedImages(latestCoworking));
        setImageFiles([]);
        setImageInputKey((current) => current + 1);
        router.refresh();
      }
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить фотографии коворкинга.');
    } finally {
      setIsUploadingPhotos(false);
    }
  }

  function moveUploadedImage(index: number, direction: -1 | 1) {
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= uploadedImages.length) {
      return;
    }
    setUploadedImages((current) => {
      const next = [...current];
      [next[index], next[targetIndex]] = [next[targetIndex], next[index]];
      return next;
    });
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
      {uploadedImages.length > 0 ? (
        <div className="mb-3">
          <div className="fw-semibold mb-2">Загруженные фотографии</div>
          <Stack direction="horizontal" gap={2} className="flex-wrap">
            {uploadedImages.map((image, index) => (
              <div key={image.fileId} className="d-flex flex-column gap-1 align-items-center">
                <img src={image.url} alt={`Фото коворкинга ${index + 1}`}
                     style={{ width: 120, height: 68, objectFit: 'cover', borderRadius: 12 }}/>
                <Stack direction="horizontal" gap={1}>
                  <Button size="sm" variant="outline-secondary" type="button"
                          disabled={index === 0 || isSubmitting || isUploadingPhotos}
                          onClick={() => moveUploadedImage(index, -1)}>Выше</Button>
                  <Button size="sm" variant="outline-secondary" type="button"
                          disabled={index === uploadedImages.length - 1 || isSubmitting || isUploadingPhotos}
                          onClick={() => moveUploadedImage(index, 1)}>Ниже</Button>
                </Stack>
              </div>
            ))}
          </Stack>
        </div>
      ) : null}
      <Form.Group className="mb-3" controlId="editCoworkingImageFiles">
        <Form.Label>Загрузить новые фотографии</Form.Label>
        <Stack direction="horizontal" gap={2} className="align-items-start flex-wrap">
          <Form.Control
            key={imageInputKey}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            multiple
            disabled={isSubmitting || isUploadingPhotos}
            onChange={handleImageFilesChange}
            style={{ maxWidth: 420 }}
          />
          <Button
            type="button"
            variant="outline-primary"
            disabled={imageFiles.length === 0 || isSubmitting || isUploadingPhotos}
            onClick={uploadSelectedPhotos}
          >
            {isUploadingPhotos ? 'Загрузка...' : 'Загрузить'}
          </Button>
        </Stack>
        <Form.Text className="text-body-secondary">
          JPG, PNG или WEBP, до 10 МБ. Нажмите «Загрузить», чтобы сразу добавить фотографии к коворкингу. Изображения
          будут обрезаны до формата 16:9 и сохранены как JPEG.
        </Form.Text>
      </Form.Group>
      <Form.Check className="mb-3" type="switch" id="editCoworkingActive" checked={active}
                  onChange={(e) => setActive(e.target.checked)} label="Коворкинг активен"/>
      <Form.Check className="mb-3" type="switch" id="editCoworkingAutoApproveMembership" checked={autoApproveMembership}
                  onChange={(e) => setAutoApproveMembership(e.target.checked)} label="Автоподтверждение участия"/>
      <Form.Check className="mb-3" type="switch" id="editCoworkingFloorMapEnabled" checked={floorMapEnabled}
                  onChange={(e) => setFloorMapEnabled(e.target.checked)}
                  label="Использовать карты этажей для бронирования"/>
      {errorMessage ? <Alert variant="danger">{errorMessage}</Alert> : null}
      <Stack direction="horizontal" gap={2} className="flex-wrap"><Button type="submit"
                                                                          disabled={isSubmitting || isUploadingPhotos}>{isSubmitting ? 'Сохранение...' : 'Сохранить изменения'}</Button><Button
        variant="outline-danger" type="button" onClick={handleArchive} disabled={isSubmitting || isUploadingPhotos}>Архивировать
        коворкинг</Button></Stack>
    </Form>
  );
}
