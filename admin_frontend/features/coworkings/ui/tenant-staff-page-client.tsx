'use client';

import { useEffect, useMemo, useState } from 'react';
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
import { TenantNav } from '@/components/tenant-nav';
import { requestJson } from '@/lib/client/api';
import type { TenantRoleDefinition, TenantRoleCode, TenantStaffMember } from '@/types/staff';
import { useAdminSession } from '@/features/session/use-admin-session';

export function TenantStaffPageClient({ coworkingId }: { coworkingId: number }) {
  const { session, isLoading: isSessionLoading, errorMessage: sessionError } = useAdminSession({ redirectToLogin: true });
  const [staff, setStaff] = useState<TenantStaffMember[]>([]);
  const [roles, setRoles] = useState<TenantRoleDefinition[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [formEmail, setFormEmail] = useState('');
  const [formRole, setFormRole] = useState<TenantRoleCode>('MANAGER');
  const [savingAccessId, setSavingAccessId] = useState<number | null>(null);

  const canManageStaff = useMemo(() => {
    return staff.some((member) => member.owner && session?.adminUserId === member.adminUserId) || session?.principalType === 'SUPERADMIN';
  }, [session, staff]);

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      requestJson<TenantStaffMember[]>(`/api/coworkings/${coworkingId}/staff`),
      requestJson<TenantRoleDefinition[]>(`/api/coworkings/${coworkingId}/staff/roles`),
    ])
      .then(([staffData, roleData]) => {
        if (!isMounted) return;
        setStaff(staffData);
        setRoles(roleData);
        if (roleData[0]) setFormRole(roleData[0].code);
      })
      .catch((error) => {
        if (!isMounted) return;
        setErrorMessage(error instanceof Error ? error.message : 'Unable to load tenant staff.');
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [coworkingId]);

  async function reloadStaff() {
    const data = await requestJson<TenantStaffMember[]>(`/api/coworkings/${coworkingId}/staff`);
    setStaff(data);
  }

  async function handleAssign(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setSubmitMessage(null);
    try {
      await requestJson<TenantStaffMember>(`/api/coworkings/${coworkingId}/staff`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: formEmail.trim(), role: formRole }),
      });
      setFormEmail('');
      await reloadStaff();
      setSubmitMessage('Staff access assigned.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to assign staff access.');
    }
  }

  async function handleUpdate(accessId: number, role: TenantRoleCode, active: boolean) {
    setSavingAccessId(accessId);
    setErrorMessage(null);
    setSubmitMessage(null);
    try {
      await requestJson<TenantStaffMember>(`/api/coworkings/${coworkingId}/staff/${accessId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ role, active }),
      });
      await reloadStaff();
      setSubmitMessage('Staff access updated.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to update staff access.');
    } finally {
      setSavingAccessId(null);
    }
  }

  async function handleDeactivate(accessId: number) {
    setSavingAccessId(accessId);
    setErrorMessage(null);
    setSubmitMessage(null);
    try {
      await requestJson<{ success: boolean }>(`/api/coworkings/${coworkingId}/staff/${accessId}`, { method: 'DELETE' });
      await reloadStaff();
      setSubmitMessage('Staff access deactivated.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to deactivate staff access.');
    } finally {
      setSavingAccessId(null);
    }
  }

  if (isSessionLoading || isLoading) return <FullPageLoader label="Loading tenant staff..." />;
  if (sessionError) return <FullPageError message={sessionError} />;
  if (!session) return <FullPageLoader label="Redirecting to login..." />;
  if (errorMessage && staff.length === 0) return <FullPageError message={errorMessage} />;

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5">
        <Stack gap={4}>
          <TenantNav coworkingId={coworkingId} />

          <Card className="content-card">
            <CardBody>
              <CardTitle as="h1" className="mb-2">Tenant staff access</CardTitle>
            </CardBody>
          </Card>

          <Card className="content-card">
            <CardBody>
              <CardTitle as="h2" className="h4 mb-3">Available role presets</CardTitle>
              <Row className="g-3">
                {roles.map((role) => (
                  <Col md={6} key={role.code}>
                    <Card bg="light">
                      <CardBody>
                        <CardTitle as="h3" className="h5">{role.label}</CardTitle>
                        <ListGroup className="mt-3">
                          {role.grantedActions.map((action) => (
                            <ListGroup.Item key={`${role.code}-${action}`}>{action}</ListGroup.Item>
                          ))}
                        </ListGroup>
                      </CardBody>
                    </Card>
                  </Col>
                ))}
              </Row>
            </CardBody>
          </Card>

          {canManageStaff ? (
            <Card className="content-card">
              <CardBody>
                <CardTitle as="h2" className="h4 mb-3">Assign staff access</CardTitle>
                <Form onSubmit={handleAssign}>
                  <Row className="g-3 align-items-end">
                    <Col md={5}>
                      <Form.Group>
                        <Form.Label>Email</Form.Label>
                        <Form.Control type="email" value={formEmail} onChange={(event) => setFormEmail(event.target.value)} required />
                      </Form.Group>
                    </Col>
                    <Col md={4}>
                      <Form.Group>
                        <Form.Label>Role</Form.Label>
                        <Form.Select value={formRole} onChange={(event) => setFormRole(event.target.value as TenantRoleCode)}>
                          {roles.map((role) => <option key={role.code} value={role.code}>{role.label}</option>)}
                        </Form.Select>
                      </Form.Group>
                    </Col>
                    <Col md={3}>
                      <Button type="submit" className="w-100">Assign access</Button>
                    </Col>
                  </Row>
                </Form>
              </CardBody>
            </Card>
          ) : null}

          <Card className="content-card">
            <CardBody>
              <CardTitle as="h2" className="h4 mb-3">Current tenant staff</CardTitle>
              {errorMessage ? <Alert variant="danger">{errorMessage}</Alert> : null}
              {submitMessage ? <Alert variant="success">{submitMessage}</Alert> : null}
              <ListGroup>
                {staff.map((member) => (
                  <ListGroup.Item key={member.accessId}>
                    <Row className="g-3 align-items-center">
                      <Col md={4}>
                        <div className="fw-semibold">{member.email}</div>
                        <Stack direction="horizontal" gap={2} className="flex-wrap mt-2">
                          <Badge bg={member.owner ? 'dark' : 'info'}>{member.owner ? 'Owner' : member.role ?? 'No role'}</Badge>
                          <Badge bg={member.active ? 'success' : 'secondary'}>{member.active ? 'Active' : 'Inactive'}</Badge>
                        </Stack>
                      </Col>
                      <Col md={5}>
                        <div className="small text-body-secondary mb-2">Granted actions</div>
                        <Stack direction="horizontal" gap={2} className="flex-wrap">
                          {member.grantedActions.map((action) => (
                            <Badge bg="light" text="dark" key={`${member.accessId}-${action}`}>{action}</Badge>
                          ))}
                        </Stack>
                      </Col>
                      <Col md={3}>
                        {canManageStaff && !member.owner ? (
                          <Stack gap={2}>
                            <Form.Select
                              value={member.role ?? 'MANAGER'}
                              onChange={(event) => handleUpdate(member.accessId, event.target.value as TenantRoleCode, member.active)}
                              disabled={savingAccessId === member.accessId}
                            >
                              {roles.map((role) => <option key={role.code} value={role.code}>{role.label}</option>)}
                            </Form.Select>
                            <Stack direction="horizontal" gap={2}>
                              <Button
                                size="sm"
                                variant={member.active ? 'outline-secondary' : 'outline-success'}
                                onClick={() => handleUpdate(member.accessId, member.role ?? 'MANAGER', !member.active)}
                                disabled={savingAccessId === member.accessId}
                              >
                                {member.active ? 'Deactivate' : 'Activate'}
                              </Button>
                              <Button
                                size="sm"
                                variant="outline-danger"
                                onClick={() => handleDeactivate(member.accessId)}
                                disabled={savingAccessId === member.accessId}
                              >
                                Remove
                              </Button>
                            </Stack>
                          </Stack>
                        ) : (
                          <CardText className="mb-0 text-body-secondary">Role is system-managed for this subject.</CardText>
                        )}
                      </Col>
                    </Row>
                  </ListGroup.Item>
                ))}
              </ListGroup>
            </CardBody>
          </Card>
        </Stack>
      </Container>
    </main>
  );
}
