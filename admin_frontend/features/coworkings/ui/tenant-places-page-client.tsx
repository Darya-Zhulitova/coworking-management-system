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
import { TenantNav } from '@/components/tenant-nav';
import { useAdminSession } from '@/features/session/use-admin-session';
import { requestJson } from '@/lib/client/api';
import type { Coworking } from '@/types/coworking';
import type {
  CoworkingConfigSnapshot,
  PlaceDeactivationPreview,
  PlaceDto,
  PlaceTypeDto,
} from '@/types/place';

interface PlacesBundle {
  coworking: Coworking;
  placeTypes: PlaceTypeDto[];
  places: PlaceDto[];
  snapshot: CoworkingConfigSnapshot;
}

export function TenantPlacesPageClient({ coworkingId }: { coworkingId: number }) {
  const { session, isLoading: isSessionLoading, errorMessage: sessionError } = useAdminSession({ redirectToLogin: true });
  const [bundle, setBundle] = useState<PlacesBundle | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [preview, setPreview] = useState<PlaceDeactivationPreview | null>(null);
  const [filterPlaceTypeId, setFilterPlaceTypeId] = useState<string>('all');

  const [typeForm, setTypeForm] = useState({ code: '', name: '', description: '' });
  const [placeForm, setPlaceForm] = useState({ name: '', placeTypeId: '' });
  const [savingKey, setSavingKey] = useState<string | null>(null);

  const canManagePlaces = useMemo(() => {
    if (!session) return false;
    const accessible = session.coworkings.find((item) => item.id === coworkingId);
    return session.principalType === 'SUPERADMIN' || accessible?.owner === true || accessible?.role === 'MANAGER';
  }, [coworkingId, session]);

  const loadData = useCallback(async () => {
    const query = filterPlaceTypeId === 'all' ? '' : `?placeTypeId=${filterPlaceTypeId}`;
    const [coworking, placeTypes, places, snapshot] = await Promise.all([
      requestJson<Coworking>(`/api/coworkings/${coworkingId}`),
      requestJson<PlaceTypeDto[]>(`/api/coworkings/${coworkingId}/place-types`),
      requestJson<PlaceDto[]>(`/api/coworkings/${coworkingId}/places${query}`),
      requestJson<CoworkingConfigSnapshot>(`/api/coworkings/${coworkingId}/config-snapshot`),
    ]);
    setBundle({ coworking, placeTypes, places, snapshot });
  }, [coworkingId, filterPlaceTypeId]);

  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);
    loadData()
      .catch((error) => {
        if (!isMounted) return;
        setErrorMessage(error instanceof Error ? error.message : 'Unable to load coworking places configuration.');
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });
    return () => {
      isMounted = false;
    };
  }, [loadData]);

  async function handleCreatePlaceType(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setSubmitMessage(null);
    setPreview(null);
    setSavingKey('create-place-type');
    try {
      await requestJson<PlaceTypeDto>(`/api/coworkings/${coworkingId}/place-types`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(typeForm),
      });
      setTypeForm({ code: '', name: '', description: '' });
      setSubmitMessage('Place type created.');
      await loadData();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to create place type.');
    } finally {
      setSavingKey(null);
    }
  }

  async function handleCreatePlace(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setSubmitMessage(null);
    setPreview(null);
    setSavingKey('create-place');
    try {
      await requestJson<PlaceDto>(`/api/coworkings/${coworkingId}/places`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: placeForm.name,
          placeTypeId: Number(placeForm.placeTypeId),
        }),
      });
      setPlaceForm({ name: '', placeTypeId: bundle?.placeTypes[0] ? String(bundle.placeTypes[0].id) : '' });
      setSubmitMessage('Place created.');
      await loadData();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to create place.');
    } finally {
      setSavingKey(null);
    }
  }

  async function handleTogglePlaceType(type: PlaceTypeDto) {
    setErrorMessage(null);
    setSubmitMessage(null);
    setPreview(null);
    setSavingKey(`type-${type.id}`);
    try {
      await requestJson<PlaceTypeDto>(`/api/coworkings/${coworkingId}/place-types/${type.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          code: type.code,
          name: type.name,
          description: type.description ?? '',
          active: !type.active,
        }),
      });
      setSubmitMessage(`Place type ${!type.active ? 'activated' : 'deactivated'}.`);
      await loadData();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to update place type.');
    } finally {
      setSavingKey(null);
    }
  }

  async function handleArchivePlaceType(type: PlaceTypeDto) {
    setErrorMessage(null);
    setSubmitMessage(null);
    setPreview(null);
    setSavingKey(`archive-type-${type.id}`);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/place-types/${type.id}`, { method: 'DELETE' });
      setSubmitMessage('Place type archived.');
      await loadData();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to archive place type.');
    } finally {
      setSavingKey(null);
    }
  }

  async function handleTogglePlace(place: PlaceDto) {
    setErrorMessage(null);
    setSubmitMessage(null);
    setPreview(null);
    setSavingKey(`place-${place.id}`);
    try {
      if (place.active) {
        const response = await requestJson<PlaceDeactivationPreview>(`/api/coworkings/${coworkingId}/places/${place.id}/deactivate`, { method: 'POST' });
        setPreview(response);
        setSubmitMessage('Place deactivated with stubbed user-domain impact preview.');
      } else {
        await requestJson<PlaceDto>(`/api/coworkings/${coworkingId}/places/${place.id}/activate`, { method: 'POST' });
        setSubmitMessage('Place activated.');
      }
      await loadData();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to change place status.');
    } finally {
      setSavingKey(null);
    }
  }

  async function handleArchivePlace(place: PlaceDto) {
    setErrorMessage(null);
    setSubmitMessage(null);
    setPreview(null);
    setSavingKey(`archive-place-${place.id}`);
    try {
      await requestJson(`/api/coworkings/${coworkingId}/places/${place.id}`, { method: 'DELETE' });
      setSubmitMessage('Place archived.');
      await loadData();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unable to archive place.');
    } finally {
      setSavingKey(null);
    }
  }

  useEffect(() => {
    if (!bundle?.placeTypes.length) return;
    setPlaceForm((current) => current.placeTypeId ? current : { ...current, placeTypeId: String(bundle.placeTypes[0].id) });
  }, [bundle?.placeTypes]);

  if (isSessionLoading || isLoading) return <FullPageLoader label="Loading place configuration..." />;
  if (sessionError) return <FullPageError message={sessionError} />;
  if (!session) return <FullPageLoader label="Redirecting to login..." />;
  if (errorMessage && !bundle) return <FullPageError message={errorMessage} />;
  if (!bundle) return <FullPageError message="Place configuration is unavailable." />;

  return (
    <main className="page-shell">
      <Container className="py-4 py-md-5">
        <Stack gap={4}>
          <TenantNav coworkingId={coworkingId} />

          <Card className="content-card">
            <Card.Body>
              <Stack gap={3}>
                <div>
                  <Card.Title as="h1" className="mb-2">{bundle.coworking.name} places configuration</Card.Title>
                  <Card.Text className="mb-0 text-body-secondary">
                    Config version: {bundle.snapshot.configVersion} · Generated at: {new Date(bundle.snapshot.generatedAt).toLocaleString()}
                  </Card.Text>
                </div>

                {submitMessage ? <Alert variant="success" className="mb-0">{submitMessage}</Alert> : null}
                {errorMessage ? <Alert variant="danger" className="mb-0">{errorMessage}</Alert> : null}
                {preview ? (
                  <Alert variant="warning" className="mb-0">
                    <div className="fw-semibold mb-2">Stub user-domain impact preview for {preview.placeName}</div>
                    <div>Affected future bookings: {preview.simulatedAffectedFutureBookings}</div>
                    <div>Mode: {preview.mode}</div>
                    <ul className="mb-0 mt-2">
                      {preview.plannedUserDomainCommands.map((item) => <li key={item}>{item}</li>)}
                    </ul>
                  </Alert>
                ) : null}
              </Stack>
            </Card.Body>
          </Card>

          <Row className="g-4">
            <Col xl={5}>
              <Card className="content-card h-100">
                <Card.Body>
                  <Stack gap={3}>
                    <div>
                      <Card.Title as="h2" className="h4 mb-2">Place types</Card.Title>
                      <Card.Text className="text-body-secondary mb-0">Tenant-specific place types, without map editor.</Card.Text>
                    </div>

                    {canManagePlaces ? (
                      <Form onSubmit={handleCreatePlaceType}>
                        <Stack gap={2}>
                          <Form.Group>
                            <Form.Label>Code</Form.Label>
                            <Form.Control value={typeForm.code} onChange={(event) => setTypeForm((current) => ({ ...current, code: event.target.value }))} placeholder="DESK" />
                          </Form.Group>
                          <Form.Group>
                            <Form.Label>Name</Form.Label>
                            <Form.Control value={typeForm.name} onChange={(event) => setTypeForm((current) => ({ ...current, name: event.target.value }))} placeholder="Desk" />
                          </Form.Group>
                          <Form.Group>
                            <Form.Label>Description</Form.Label>
                            <Form.Control value={typeForm.description} onChange={(event) => setTypeForm((current) => ({ ...current, description: event.target.value }))} placeholder="Optional description" />
                          </Form.Group>
                          <Button type="submit" disabled={savingKey === 'create-place-type'}>Create place type</Button>
                        </Stack>
                      </Form>
                    ) : (
                      <Alert variant="secondary" className="mb-0">Read-only access. Managing place types requires MANAGE_PLACES.</Alert>
                    )}

                    <Table responsive hover>
                      <thead>
                        <tr>
                          <th>Name</th>
                          <th>Code</th>
                          <th>Status</th>
                          {canManagePlaces ? <th></th> : null}
                        </tr>
                      </thead>
                      <tbody>
                        {bundle.placeTypes.map((type) => (
                          <tr key={type.id}>
                            <td>
                              <div className="fw-semibold">{type.name}</div>
                              {type.description ? <div className="small text-body-secondary">{type.description}</div> : null}
                            </td>
                            <td>{type.code}</td>
                            <td>
                              <Stack direction="horizontal" gap={2} className="flex-wrap">
                                <Badge bg={type.active ? 'success' : 'secondary'}>{type.active ? 'Active' : 'Inactive'}</Badge>
                                <Badge bg={type.archived ? 'dark' : 'info'}>{type.archived ? 'Archived' : 'Current'}</Badge>
                              </Stack>
                            </td>
                            {canManagePlaces ? (
                              <td>
                                <Stack direction="horizontal" gap={2} className="justify-content-end flex-wrap">
                                  <Button size="sm" variant="outline-primary" disabled={savingKey === `type-${type.id}`} onClick={() => handleTogglePlaceType(type)}>
                                    {type.active ? 'Deactivate' : 'Activate'}
                                  </Button>
                                  <Button size="sm" variant="outline-danger" disabled={savingKey === `archive-type-${type.id}`} onClick={() => handleArchivePlaceType(type)}>
                                    Archive
                                  </Button>
                                </Stack>
                              </td>
                            ) : null}
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  </Stack>
                </Card.Body>
              </Card>
            </Col>

            <Col xl={7}>
              <Card className="content-card h-100">
                <Card.Body>
                  <Stack gap={3}>
                    <div>
                      <Card.Title as="h2" className="h4 mb-2">Places</Card.Title>
                      <Card.Text className="text-body-secondary mb-0">Tabular place configuration with activation, deactivation and archive flows.</Card.Text>
                    </div>

                    <Form className="d-flex flex-wrap gap-2 align-items-end">
                      <Form.Group>
                        <Form.Label>Filter by type</Form.Label>
                        <Form.Select value={filterPlaceTypeId} onChange={(event) => setFilterPlaceTypeId(event.target.value)}>
                          <option value="all">All types</option>
                          {bundle.placeTypes.map((type) => <option key={type.id} value={type.id}>{type.name}</option>)}
                        </Form.Select>
                      </Form.Group>
                    </Form>

                    {canManagePlaces ? (
                      <Form onSubmit={handleCreatePlace}>
                        <Row className="g-2 align-items-end">
                          <Col md={5}>
                            <Form.Group>
                              <Form.Label>Name</Form.Label>
                              <Form.Control value={placeForm.name} onChange={(event) => setPlaceForm((current) => ({ ...current, name: event.target.value }))} placeholder="D-03" />
                            </Form.Group>
                          </Col>
                          <Col md={4}>
                            <Form.Group>
                              <Form.Label>Place type</Form.Label>
                              <Form.Select value={placeForm.placeTypeId} onChange={(event) => setPlaceForm((current) => ({ ...current, placeTypeId: event.target.value }))}>
                                {bundle.placeTypes.map((type) => <option key={type.id} value={type.id}>{type.name}</option>)}
                              </Form.Select>
                            </Form.Group>
                          </Col>
                          <Col md={3}>
                            <Button type="submit" className="w-100" disabled={savingKey === 'create-place' || !bundle.placeTypes.length}>Create place</Button>
                          </Col>
                        </Row>
                      </Form>
                    ) : null}

                    <Table responsive hover>
                      <thead>
                        <tr>
                          <th>Name</th>
                          <th>Type</th>
                          <th>Status</th>
                          {canManagePlaces ? <th></th> : null}
                        </tr>
                      </thead>
                      <tbody>
                        {bundle.places.map((place) => (
                          <tr key={place.id}>
                            <td>
                              <div className="fw-semibold">{place.name}</div>
                              <div className="small text-body-secondary">ID: {place.id}</div>
                            </td>
                            <td>
                              <div>{place.placeType.name}</div>
                              <div className="small text-body-secondary">{place.placeType.code}</div>
                            </td>
                            <td>
                              <Stack direction="horizontal" gap={2} className="flex-wrap">
                                <Badge bg={place.active ? 'success' : 'secondary'}>{place.active ? 'Active' : 'Inactive'}</Badge>
                                <Badge bg={place.archived ? 'dark' : 'info'}>{place.archived ? 'Archived' : 'Current'}</Badge>
                              </Stack>
                            </td>
                            {canManagePlaces ? (
                              <td>
                                <Stack direction="horizontal" gap={2} className="justify-content-end flex-wrap">
                                  <Button size="sm" variant={place.active ? 'outline-warning' : 'outline-success'} disabled={savingKey === `place-${place.id}`} onClick={() => handleTogglePlace(place)}>
                                    {place.active ? 'Deactivate' : 'Activate'}
                                  </Button>
                                  <Button size="sm" variant="outline-danger" disabled={savingKey === `archive-place-${place.id}`} onClick={() => handleArchivePlace(place)}>
                                    Archive
                                  </Button>
                                </Stack>
                              </td>
                            ) : null}
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  </Stack>
                </Card.Body>
              </Card>
            </Col>
          </Row>

          <Card className="content-card">
            <Card.Body>
              <Stack gap={3}>
                <div>
                  <Card.Title as="h2" className="h4 mb-2">Read-only config snapshot</Card.Title>
                  <Card.Text className="text-body-secondary mb-0">Owner contract preview for the future user-domain consumer.</Card.Text>
                </div>
                <pre className="mb-0 p-3 rounded bg-light border small overflow-auto">{JSON.stringify(bundle.snapshot, null, 2)}</pre>
              </Stack>
            </Card.Body>
          </Card>
        </Stack>
      </Container>
    </main>
  );
}
