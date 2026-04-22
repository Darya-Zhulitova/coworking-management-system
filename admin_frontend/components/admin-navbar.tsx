'use client';

import Link from 'next/link';
import { useMemo } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import Container from 'react-bootstrap/Container';
import Nav from 'react-bootstrap/Nav';
import Navbar from 'react-bootstrap/Navbar';
import Spinner from 'react-bootstrap/Spinner';
import { useAppContext } from '@/features/context/use-app-context';
import { formatRoleLabel } from '@/lib/format/labels';

interface NavigationItem {
  href: string;
  label: string;
  active: boolean;
}

function buildNavigationItems(coworkingId: number | null, grants: string[], pathname: string | null): NavigationItem[] {
  const items: NavigationItem[] = [];

  if (coworkingId == null) {
    return items;
  }

  const activeStarts = (suffix: string) => pathname === suffix || pathname?.startsWith(`${suffix}/`) === true;
  const canViewUsers = grants.includes('USER_READ');
  const canViewSettings = grants.some((grant) => ['COWORKING_READ', 'FLOOR_READ', 'PLACE_TYPE_READ', 'TARIFF_READ', 'SCHEDULE_READ', 'SERVICE_REQUEST_TYPE_READ'].includes(grant));
  const canViewStaff = grants.includes('ROLE_READ') || grants.includes('ACCESS_READ');

  if (canViewUsers) {
    items.push({
      href: `/coworkings/${coworkingId}/users`,
      label: 'Управление',
      active: activeStarts(`/coworkings/${coworkingId}/users`) || activeStarts(`/coworkings/${coworkingId}/membership-queue`) || activeStarts(`/coworkings/${coworkingId}/pay-requests`) || activeStarts(`/coworkings/${coworkingId}/service-requests`),
    });
  }
  if (canViewSettings) {
    items.push({
      href: `/coworkings/${coworkingId}/settings`,
      label: 'Настройки',
      active: activeStarts(`/coworkings/${coworkingId}/settings`) || activeStarts(`/coworkings/${coworkingId}/floors`) || activeStarts(`/coworkings/${coworkingId}/places`) || activeStarts(`/coworkings/${coworkingId}/place-types`) || activeStarts(`/coworkings/${coworkingId}/tariffs`) || activeStarts(`/coworkings/${coworkingId}/service-request-types`) || activeStarts(`/coworkings/${coworkingId}/schedule`),
    });
  }
  if (canViewStaff) {
    items.push({
      href: `/coworkings/${coworkingId}/staff`,
      label: 'Сотрудники',
      active: activeStarts(`/coworkings/${coworkingId}/staff`) || activeStarts(`/coworkings/${coworkingId}/roles`),
    });
  }

  return items;
}

export function AdminNavbar() {
  const pathname = usePathname();
  const router = useRouter();
  const selectedCoworkingId = useMemo(() => {
    if (pathname == null) return undefined;
    const match = pathname.match(/^\/coworkings\/(\d+)/);
    if (!match) return undefined;
    const parsed = Number(match[1]);
    return Number.isInteger(parsed) ? parsed : undefined;
  }, [pathname]);
  const shouldHideNavbar = pathname === '/login';
  const { context, isLoading } = useAppContext({ coworkingId: selectedCoworkingId });

  const navigationItems = useMemo(() => {
    if (!context) return [];
    return buildNavigationItems(context.coworkingId ?? null, context.grants, pathname);
  }, [context, pathname]);

  async function handleLogout() {
    await fetch('/api/auth/logout', { method: 'POST' });
    router.replace('/login');
    router.refresh();
  }

  if (shouldHideNavbar) {
    return null;
  }

  return (
    <Navbar expand="lg" bg="white" className="admin-navbar border-bottom sticky-top" collapseOnSelect>
      <Container fluid="xl" className="px-3 px-md-4">
        <Navbar.Brand as={Link} href="/" className="fw-semibold d-flex align-items-center gap-2 me-0">
          <span>SpaceBooking Manager</span>
          {context?.coworkingName ? (
            <>
              <span className="text-body-tertiary navbar-separator">|</span>
              <span className="text-body-secondary text-wrap admin-navbar-coworking">{context.coworkingName}</span>
            </>
          ) : null}
        </Navbar.Brand>

        <Navbar.Toggle aria-controls="admin-navbar-nav"/>
        <Navbar.Collapse id="admin-navbar-nav">
          <Nav className="ms-auto align-items-lg-center gap-lg-2 admin-navbar-links">
            {navigationItems.map((item) => (
              <Nav.Link as={Link} href={item.href} key={item.href} active={item.active}>
                {item.label}
              </Nav.Link>
            ))}
          </Nav>

          {context || isLoading ? <div
            className="admin-navbar-meta ms-lg-3 ps-lg-3 mt-3 mt-lg-0 d-flex align-items-center justify-content-lg-end gap-3 border-lg-0 pt-3 pt-lg-0">
            {isLoading ? (
              <div className="d-inline-flex align-items-center gap-2 text-body-secondary small">
                <Spinner animation="border" size="sm"/>
                <span>Загрузка профиля</span>
              </div>
            ) : context ? (
              <div className="text-body-secondary text-lg-end small">
                <span
                  className="d-inline-block">{context.name}{context.role ? ` [${formatRoleLabel(context.role)}]` : ''}</span>
              </div>
            ) : null}
            {context ? <button
              type="button"
              onClick={handleLogout}
              className="btn btn-link p-0 text-decoration-none admin-navbar-logout"
              aria-label="Выйти"
              title="Выйти"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" viewBox="0 0 16 16"
                   aria-hidden="true">
                <path fillRule="evenodd"
                      d="M10.146 11.354a.5.5 0 0 1 0-.708L12.293 8.5H5.5a.5.5 0 0 1 0-1h6.793l-2.147-2.146a.5.5 0 1 1 .708-.708l3 3a.5.5 0 0 1 0 .708l-3 3a.5.5 0 0 1-.708 0"/>
                <path fillRule="evenodd"
                      d="M13.5 15a.5.5 0 0 0 .5-.5v-13a.5.5 0 0 0-.5-.5h-8a.5.5 0 0 0-.5.5V4a.5.5 0 0 1-1 0V1.5A1.5 1.5 0 0 1 5.5 0h8A1.5 1.5 0 0 1 15 1.5v13a1.5 1.5 0 0 1-1.5 1.5h-8A1.5 1.5 0 0 1 4 14.5V12a.5.5 0 0 1 1 0v2.5a.5.5 0 0 0 .5.5z"/>
              </svg>
            </button> : null}
          </div> : null}
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
