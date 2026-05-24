'use client';

import { useEffect, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import CardBody from 'react-bootstrap/CardBody';
import CardTitle from 'react-bootstrap/CardTitle';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import ListGroup from 'react-bootstrap/ListGroup';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import type { CoworkingAccessItem, CoworkingRoleDefinition } from '@/types/staff';
import { formatActiveStatus, formatGrantList, formatRoleLabel } from '@/lib/format/labels';

export function CoworkingStaffListPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({
    coworkingId,
    redirectToLogin: true
  });
  const [accessList, setAccessList] = useState<CoworkingAccessItem[]>([]);
  const [roles, setRoles] = useState<CoworkingRoleDefinition[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [formEmail, setFormEmail] = useState('');
  const [formRoleId, setFormRoleId] = useState<number | ''>('');
  const [savingAccessId, setSavingAccessId] = useState<number | null>(null);
  const canManageAccess = context?.grants.includes('ACCESS_EDIT') ?? false;

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      requestJson<CoworkingAccessItem[]>(`/api/coworkings/${coworkingId}/staff`),
      requestJson<CoworkingRoleDefinition[]>(`/api/coworkings/${coworkingId}/staff/roles`),
    ]).then(([staffData, roleData]) => {
      if (!isMounted) return;
      setAccessList(staffData);
      setRoles(roleData);
      const activeRoles = roleData.filter((role) => role.active);
      if (activeRoles[0]) setFormRoleId(activeRoles[0].roleId);
    }).catch((error) => {
      if (isMounted) setErrorMessage(error instanceof Error ? error.message : 'Не удалось загрузить список доступов.');
    }).finally(() => {
      if (isMounted) setIsLoading(false);
    });
    return () => {
      isMounted = false;
    };
  }, [coworkingId]);

  async function reloadStaff() {
    setAccessList(await requestJson<CoworkingAccessItem[]>(`/api/coworkings/${coworkingId}/staff`));
  }

  async function handleAssign(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await requestJson(`/api/coworkings/${coworkingId}/staff`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: formEmail.trim(), roleId: formRoleId })
      });
      setFormEmail('');
      await reloadStaff();
      setSubmitMessage('Доступ назначен.');
    } catch (e) {
      setErrorMessage(e instanceof Error ? e.message : 'Не удалось выдать доступ.');
    }
  }

  async function handleUpdate(accessId: number, roleId: number, active: boolean) {
    setSavingAccessId(accessId);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/staff/${accessId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ roleId, active })
      });
      await reloadStaff();
      setSubmitMessage('Access updated.');
    } catch (e) {
      setErrorMessage(e instanceof Error ? e.message : 'Не удалось обновить доступ.');
    } finally {
      setSavingAccessId(null);
    }
  }

  async function handleDeactivate(accessId: number) {
    setSavingAccessId(accessId);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/staff/${accessId}`, { method: 'DELETE' });
      await reloadStaff();
      setSubmitMessage('Access deactivated.');
    } catch (e) {
      setErrorMessage(e instanceof Error ? e.message : 'Не удалось удалить доступ.');
    } finally {
      setSavingAccessId(null);
    }
  }

  if (isContextLoading || isLoading) return <FullPageLoader label="Загрузка доступа сотрудников..."/>;
  if (contextError) return <FullPageError message={contextError}/>;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Переход на страницу входа..."/>;
  return <main className="page-shell"><Container className="py-4 py-md-5"><Stack gap={4}>
    <div><h2 className="mb-2">Сотрудники</h2><p className="mb-0 text-body-secondary">Управление назначением доступов
      сотрудников.</p></div>
    {submitMessage ?
      <Alert variant="success" className="mb-0">{submitMessage}</Alert> : null}{errorMessage ?
    <Alert variant="danger" className="mb-0">{errorMessage}</Alert> : null}<Row className="g-4"><Col lg={5}><Card
    className="content-card h-100"><CardBody><CardTitle as="h2" className="h4 mb-3">Выдать роль
    сотруднику</CardTitle>{canManageAccess ?
    <Form onSubmit={handleAssign}><Stack gap={3}><Form.Group><Form.Label>Email администратора</Form.Label><Form.Control
      value={formEmail} onChange={(e) => setFormEmail(e.target.value)}
      required/></Form.Group><Form.Group><Form.Label>Роль</Form.Label><Form.Select value={formRoleId}
                                                                                   onChange={(e) => setFormRoleId(Number(e.target.value))}
                                                                                   required>{roles.filter((role) => role.active).map((role) =>
      <option key={role.roleId} value={role.roleId}>{role.name}</option>)}</Form.Select></Form.Group><Button
      type="submit">Выдать доступ</Button></Stack></Form> :
    <Alert variant="secondary" className="mb-0">Недостаточно прав для изменения.</Alert>}</CardBody></Card></Col><Col
    lg={7}><Card className="content-card h-100"><CardBody><CardTitle as="h2" className="h4 mb-3">Назначенные
    доступы</CardTitle><ListGroup variant="flush">{accessList.map((member) => <ListGroup.Item key={member.accessId}
                                                                                              className="px-0 entity-card"><Stack
    gap={3}>
    <div>
      <div className="fw-semibold fs-5">{member.name}</div>
      <div className="text-body-secondary">{member.email}</div>
      <Stack direction="horizontal" gap={2} className="flex-wrap mt-2"><Badge
        bg={member.active ? 'success' : 'secondary'}>{formatActiveStatus(member.active)}</Badge><Badge
        bg="info">{formatRoleLabel(member.roleName)}</Badge></Stack>
      <div className="small text-body-secondary mt-2">Права: {formatGrantList(member.grants)}</div>
    </div>
    {canManageAccess ?
      <Stack direction="horizontal" gap={2} className="flex-wrap align-items-center"><Form.Select size="sm"
                                                                                                  style={{ maxWidth: 240 }}
                                                                                                  defaultValue={member.roleId}
                                                                                                  onChange={(e) => handleUpdate(member.accessId, Number(e.target.value), member.active)}
                                                                                                  disabled={savingAccessId === member.accessId}>{roles.filter((role) => role.active).map((role) =>
        <option key={role.roleId} value={role.roleId}>{role.name}</option>)}</Form.Select><Button size="sm"
                                                                                                  variant="outline-primary"
                                                                                                  disabled={savingAccessId === member.accessId}
                                                                                                  onClick={() => handleUpdate(member.accessId, member.roleId, !member.active)}>{member.active ? 'Деактивировать' : 'Активировать'}</Button><Button
        size="sm" variant="outline-danger" disabled={savingAccessId === member.accessId}
        onClick={() => handleDeactivate(member.accessId)}>Удалить доступ</Button></Stack> : null}
  </Stack></ListGroup.Item>)}</ListGroup></CardBody></Card></Col></Row></Stack></Container></main>;
}
