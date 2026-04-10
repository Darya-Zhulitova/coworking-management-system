'use client';

import { useEffect, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import CardBody from 'react-bootstrap/CardBody';
import CardText from 'react-bootstrap/CardText';
import CardTitle from 'react-bootstrap/CardTitle';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import ListGroup from 'react-bootstrap/ListGroup';
import Row from 'react-bootstrap/Row';
import Stack from 'react-bootstrap/Stack';
import { FullPageError, FullPageLoader } from '@/components/page-state';
import { CoworkingNav } from '@/components/coworking-nav';
import { useAppContext } from '@/features/context/use-app-context';
import { requestJson } from '@/lib/client/api';
import type { CoworkingRoleDefinition, CoworkingAccessItem } from '@/types/staff';

export function CoworkingStaffPageClient({ coworkingId }: { coworkingId: number }) {
  const { context, isLoading: isContextLoading, errorMessage: contextError } = useAppContext({ coworkingId, redirectToLogin: true });
  const [accessList, setAccessList] = useState<CoworkingAccessItem[]>([]);
  const [roles, setRoles] = useState<CoworkingRoleDefinition[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [formEmail, setFormEmail] = useState('');
  const [formRoleId, setFormRoleId] = useState<number | ''>('');
  const [savingAccessId, setSavingAccessId] = useState<number | null>(null);

  const canManageAccess = context?.grants.includes('ACCESS_MANAGE') ?? false;

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      requestJson<CoworkingAccessItem[]>(`/api/coworkings/${coworkingId}/staff`),
      requestJson<CoworkingRoleDefinition[]>(`/api/coworkings/${coworkingId}/staff/roles`),
    ])
      .then(([staffData, roleData]) => {
        if (!isMounted) return;
        setAccessList(staffData);
        setRoles(roleData);
        if (roleData[0]) setFormRoleId(roleData[0].roleId);
      })
      .catch((error) => {
        if (!isMounted) return;
        setErrorMessage(error instanceof Error ? error.message : 'Unable to load coworking access list.');
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [coworkingId]);

  async function reloadStaff() {
    const data = await requestJson<CoworkingAccessItem[]>(`/api/coworkings/${coworkingId}/staff`);
    setAccessList(data);
  }

  async function handleAssign(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setSubmitMessage(null);
    try {
      await requestJson<CoworkingAccessItem>(`/api/coworkings/${coworkingId}/staff`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: formEmail.trim(), roleId: formRoleId }),
      });
      setFormEmail('');
      await reloadStaff();
      setSubmitMessage('Access assigned.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to assign access.');
    }
  }

  async function handleUpdate(accessId: number, roleId: number, active: boolean) {
    setSavingAccessId(accessId);
    setErrorMessage(null);
    setSubmitMessage(null);
    try {
      await requestJson<CoworkingAccessItem>(`/api/coworkings/${coworkingId}/staff/${accessId}`, {
        method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ roleId, active }),
      });
      await reloadStaff();
      setSubmitMessage('Access updated.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to update access.');
    } finally {
      setSavingAccessId(null);
    }
  }

  async function handleDeactivate(accessId: number) {
    setSavingAccessId(accessId);
    setErrorMessage(null);
    setSubmitMessage(null);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/staff/${accessId}`, { method: 'DELETE' });
      await reloadStaff();
      setSubmitMessage('Access deactivated.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to deactivate access.');
    } finally {
      setSavingAccessId(null);
    }
  }

  if (isContextLoading || isLoading) return <FullPageLoader label="Loading coworking access..." />;
  if (contextError) return <FullPageError message={contextError} />;
  if (!context || context.coworkingId == null) return <FullPageLoader label="Redirecting to login..." />;

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5">
        <Stack gap={4}>
          <CoworkingNav coworkingId={coworkingId} grants={context.grants} />
          <Card className="content-card">
            <CardBody>
              <CardTitle as="h1" className="mb-2">{context.coworkingName} access</CardTitle>
              <CardText className="mb-0 text-body-secondary">Resolved access: {context.role}</CardText>
            </CardBody>
          </Card>

          {submitMessage ? <Alert variant="success" className="mb-0">{submitMessage}</Alert> : null}
          {errorMessage ? <Alert variant="danger" className="mb-0">{errorMessage}</Alert> : null}

          <Row className="g-4">
            <Col lg={5}>
              <Card className="content-card h-100">
                <CardBody>
                  <CardTitle as="h2" className="h4 mb-3">Assign access role</CardTitle>
                  {canManageAccess ? (
                    <Form onSubmit={handleAssign}>
                      <Stack gap={3}>
                        <Form.Group>
                          <Form.Label>Admin email</Form.Label>
                          <Form.Control value={formEmail} onChange={(event) => setFormEmail(event.target.value)} placeholder="admin@test.test" required />
                        </Form.Group>
                        <Form.Group>
                          <Form.Label>Role</Form.Label>
                          <Form.Select value={formRoleId} onChange={(event) => setFormRoleId(Number(event.target.value))} required>
                            {roles.map((role) => <option key={role.roleId} value={role.roleId}>{role.name}</option>)}
                          </Form.Select>
                        </Form.Group>
                        <Button type="submit">Assign access</Button>
                      </Stack>
                    </Form>
                  ) : (
                    <Alert variant="secondary" className="mb-0">Read-only access. Managing staff requires ACCESS_MANAGE.</Alert>
                  )}
                </CardBody>
              </Card>
            </Col>
            <Col lg={7}>
              <Card className="content-card h-100">
                <CardBody>
                  <CardTitle as="h2" className="h4 mb-3">Assigned access</CardTitle>
                  {accessList.length > 0 ? (
                    <ListGroup variant="flush">
                      {accessList.map((member) => (
                        <ListGroup.Item key={member.accessId} className="px-0 entity-card">
                          <Stack gap={3}>
                            <div>
                              <div className="fw-semibold fs-5">{member.name}</div>
                              <div className="text-body-secondary">{member.email}</div>
                              <Stack direction="horizontal" gap={2} className="flex-wrap mt-2">
                                <Badge bg={member.active ? 'success' : 'secondary'}>{member.active ? 'Active' : 'Inactive'}</Badge>
                                <Badge bg="info">{member.roleName}</Badge>
                              </Stack>
                              <div className="small text-body-secondary mt-2">Grants: {member.grants.join(', ')}</div>
                            </div>
                            {canManageAccess ? (
                              <Stack direction="horizontal" gap={2} className="flex-wrap align-items-center">
                                <Form.Select
                                  size="sm"
                                  style={{ maxWidth: 240 }}
                                  defaultValue={member.roleId}
                                  onChange={(event) => handleUpdate(member.accessId, Number(event.target.value), member.active)}
                                  disabled={savingAccessId === member.accessId}
                                >
                                  {roles.map((role) => <option key={role.roleId} value={role.roleId}>{role.name}</option>)}
                                </Form.Select>
                                <Button size="sm" variant="outline-primary" disabled={savingAccessId === member.accessId} onClick={() => handleUpdate(member.accessId, member.roleId, !member.active)}>
                                  {member.active ? 'Deactivate' : 'Activate'}
                                </Button>
                                <Button size="sm" variant="outline-danger" disabled={savingAccessId === member.accessId} onClick={() => handleDeactivate(member.accessId)}>
                                  Remove access
                                </Button>
                              </Stack>
                            ) : null}
                          </Stack>
                        </ListGroup.Item>
                      ))}
                    </ListGroup>
                  ) : (
                    <Alert variant="secondary" className="mb-0">No access assignments in this coworking.</Alert>
                  )}
                </CardBody>
              </Card>
            </Col>
          </Row>
        </Stack>
      </Container>
    </main>
  );
}
