'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import Table from 'react-bootstrap/Table';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import type { CoworkingRoleDefinition } from '@/types/staff';
import { formatActiveStatus, formatGrantLabel, formatGrantList } from '@/lib/format/labels';

const grantOptions = [
  'COWORKING_READ', 'COWORKING_EDIT', 'FLOOR_READ', 'FLOOR_EDIT', 'PLACE_TYPE_READ', 'PLACE_TYPE_EDIT', 'PLACE_READ', 'PLACE_EDIT', 'TARIFF_READ', 'TARIFF_EDIT', 'SERVICE_REQUEST_TYPE_READ', 'SERVICE_REQUEST_TYPE_EDIT', 'ROLE_READ', 'ROLE_EDIT', 'ACCESS_READ', 'ACCESS_EDIT', 'SCHEDULE_READ', 'SCHEDULE_EDIT', 'USER_READ', 'USER_EDIT', 'BOOKING_READ', 'BOOKING_EDIT',
] as const;

export function CoworkingRolesPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [roles, setRoles] = useState<CoworkingRoleDefinition[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [savingRoleId, setSavingRoleId] = useState<number | 'create' | null>(null);
  const [editingRoleId, setEditingRoleId] = useState<number | null>(null);
  const [formName, setFormName] = useState('');
  const [selectedGrants, setSelectedGrants] = useState<string[]>(['ROLE_READ']);
  const [formActive, setFormActive] = useState(true);
  const canManageRoles = useMemo(() => context?.grants.includes('ROLE_EDIT') ?? false, [context]);
  const loadRoles = useCallback(async () => setRoles(await requestJson<CoworkingRoleDefinition[]>(`/api/coworkings/${coworkingId}/staff/roles`)), [coworkingId]);
  useEffect(() => {
    let mounted = true;
    loadRoles().catch((e) => mounted && setErrorMessage(e instanceof Error ? e.message : 'Не удалось загрузить роли.')).finally(() => mounted && setIsLoading(false));
    return () => {
      mounted = false
    };
  }, [loadRoles]);
  const resetForm = () => {
    setEditingRoleId(null);
    setFormName('');
    setSelectedGrants(['ROLE_READ']);
    setFormActive(true);
  };
  const loadRoleIntoForm = (role: CoworkingRoleDefinition) => {
    setEditingRoleId(role.roleId);
    setFormName(role.name);
    setSelectedGrants(role.grants);
    setFormActive(role.active);
  };

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const mode = editingRoleId ?? 'create';
    setSavingRoleId(mode);
    setErrorMessage(null);
    setSubmitMessage(null);
    try {
      const path = editingRoleId == null ? `/api/coworkings/${coworkingId}/staff/roles` : `/api/coworkings/${coworkingId}/staff/roles/${editingRoleId}`;
      await requestJson(path, {
        method: editingRoleId == null ? 'POST' : 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: formName, grants: selectedGrants, active: formActive })
      });
      await loadRoles();
      setSubmitMessage(editingRoleId == null ? 'Роль создана.' : 'Роль обновлена.');
      resetForm();
    } catch (e) {
      setErrorMessage(e instanceof Error ? e.message : 'Не удалось сохранить роль.');
    } finally {
      setSavingRoleId(null);
    }
  }

  async function archiveRole(role: CoworkingRoleDefinition) {
    setSavingRoleId(role.roleId);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/staff/roles/${role.roleId}`, { method: 'DELETE' });
      await loadRoles();
      setSubmitMessage('Роль отправлена в архив.');
      if (editingRoleId === role.roleId) resetForm();
    } catch (e) {
      setErrorMessage(e instanceof Error ? e.message : 'Не удалось отправить роль в архив.');
    } finally {
      setSavingRoleId(null);
    }
  }

  if (isContextLoading || isLoading) return <FullPageLoader label="Загрузка ролей..."/>;
  if (contextError) return <FullPageError message={contextError}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;
  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}>
    <div><h2 className="mb-2">Роли</h2><p className="mb-0 text-body-secondary">Создание и управление ролями отдельно от
      назначений сотрудников.</p></div>
    {submitMessage ?
      <Alert variant="success" className="mb-0">{submitMessage}</Alert> : null}{errorMessage ?
    <Alert variant="danger" className="mb-0">{errorMessage}</Alert> : null}<Row className="g-4"><Col lg={5}><Card
    className="content-card h-100"><Card.Body><Card.Title as="h2"
                                                          className="h4 mb-3">{editingRoleId == null ? 'Создать роль' : 'Редактировать роль'}</Card.Title>{canManageRoles ?
    <Form onSubmit={handleSubmit}><Stack gap={3}><Form.Group><Form.Label>Название роли</Form.Label><Form.Control
      value={formName} onChange={(e) => setFormName(e.target.value)} required/></Form.Group>
      <div><Form.Label className="d-block mb-2">Права</Form.Label>
        <div className="d-flex flex-wrap gap-2">{grantOptions.map((grant) => <Form.Check key={grant} inline
                                                                                         type="checkbox" id={grant}
                                                                                         label={formatGrantLabel(grant)}
                                                                                         checked={selectedGrants.includes(grant)}
                                                                                         onChange={(e) => setSelectedGrants((current) => e.target.checked ? [...current, grant] : current.filter((item) => item !== grant))}/>)}</div>
      </div>
      <Form.Check label="Роль активна" checked={formActive} onChange={(e) => setFormActive(e.target.checked)}/><Stack
        direction="horizontal" gap={2}><Button type="submit"
                                               disabled={savingRoleId !== null}>{editingRoleId == null ? 'Создать роль' : 'Сохранить изменения'}</Button>{editingRoleId != null ?
        <Button variant="outline-secondary" onClick={resetForm}>Отмена</Button> : null}</Stack></Stack></Form> :
    <Alert variant="secondary" className="mb-0">Только просмотр. Для создания ролей требуется право «Редактирование
      ролей».</Alert>}
  </Card.Body></Card></Col><Col lg={7}><Card className="content-card h-100"><Card.Body><Card.Title as="h2"
                                                                                                   className="h4 mb-3">Список
    ролей</Card.Title><Table responsive hover>
    <thead>
    <tr>
      <th>Название</th>
      <th>Статус</th>
      <th>Права</th>
      <th>Действия</th>
    </tr>
    </thead>
    <tbody>{roles.map((role) => <tr key={role.roleId}>
      <td>{role.name}</td>
      <td><Badge bg={role.active ? 'success' : 'secondary'}>{formatActiveStatus(role.active)}</Badge></td>
      <td className="small">{formatGrantList(role.grants)}</td>
      <td>{canManageRoles ? <Stack direction="horizontal" gap={2}><Button size="sm" variant="outline-primary"
                                                                          onClick={() => loadRoleIntoForm(role)}>Изменить</Button><Button
        size="sm" variant="outline-danger" onClick={() => archiveRole(role)} disabled={savingRoleId === role.roleId}>В
        архив</Button></Stack> : '—'}</td>
    </tr>)}</tbody>
  </Table></Card.Body></Card></Col></Row></Stack></Container></main>;
}
