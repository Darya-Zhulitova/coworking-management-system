'use client';

import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import InputGroup from 'react-bootstrap/InputGroup';
import Stack from 'react-bootstrap/Stack';

interface Props {
  imageUrls: string[];
  onChange: (next: string[]) => void;
  controlIdPrefix: string;
}

export function CoworkingImageLinksEditor({ imageUrls, onChange, controlIdPrefix }: Props) {
  function updateItem(index: number, value: string) {
    onChange(imageUrls.map((item, currentIndex) => currentIndex === index ? value : item));
  }

  function removeItem(index: number) {
    onChange(imageUrls.filter((_, currentIndex) => currentIndex !== index));
  }

  function moveItem(index: number, direction: -1 | 1) {
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= imageUrls.length) {
      return;
    }
    const next = [...imageUrls];
    const [item] = next.splice(index, 1);
    next.splice(targetIndex, 0, item);
    onChange(next);
  }

  function addItem() {
    onChange([...imageUrls, '']);
  }

  return (
    <Stack gap={3}>
      <div>
        <div className="fw-semibold mb-1">Изображения для карусели</div>
        <div className="text-body-secondary small">
          Добавьте ссылки на изображения. Порядок в списке станет порядком в карусели пользователя.
        </div>
      </div>

      {imageUrls.length > 0 ? imageUrls.map((imageUrl, index) => (
        <Form.Group key={`${controlIdPrefix}-${index}`} controlId={`${controlIdPrefix}-${index}`}>
          <Form.Label>Ссылка {index + 1}</Form.Label>
          <InputGroup>
            <Form.Control
              type="url"
              placeholder="https://example.com/coworking-photo.jpg"
              value={imageUrl}
              onChange={(event) => updateItem(index, event.target.value)}
            />
            <Button
              variant="outline-secondary"
              type="button"
              onClick={() => moveItem(index, -1)}
              disabled={index === 0}
            >
              ↑
            </Button>
            <Button
              variant="outline-secondary"
              type="button"
              onClick={() => moveItem(index, 1)}
              disabled={index === imageUrls.length - 1}
            >
              ↓
            </Button>
            <Button variant="outline-danger" type="button" onClick={() => removeItem(index)}>
              Удалить
            </Button>
          </InputGroup>
        </Form.Group>
      )) : (
        <div className="text-body-secondary small">Пока нет ни одной ссылки.</div>
      )}

      <div>
        <Button variant="outline-primary" type="button" onClick={addItem}>
          Добавить ссылку
        </Button>
      </div>
    </Stack>
  );
}
