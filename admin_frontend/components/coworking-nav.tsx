'use client';

import Nav from 'react-bootstrap/Nav';
import { usePathname } from 'next/navigation';
import Link from 'next/link';

export function CoworkingNav({ coworkingId, grants }: { coworkingId: number; grants: string[] }) {
  const pathname = usePathname();
  const canViewDashboard = grants.includes('COWORKING_DASHBOARD_VIEW');
  const canViewDetails = grants.includes('COWORKING_VIEW');
  const canViewPlaces = grants.includes('PLACE_VIEW');
  const canViewAccess = grants.includes('ACCESS_VIEW');

  return (
    <Nav variant="tabs" className="coworking-nav">
      <Nav.Item>
        <Nav.Link as={Link} href="/coworkings" active={pathname === '/coworkings'}>All coworkings</Nav.Link>
      </Nav.Item>
      {canViewDashboard ? (
        <Nav.Item>
          <Nav.Link as={Link} href={`/coworkings/${coworkingId}/dashboard`} active={pathname === `/coworkings/${coworkingId}/dashboard`}>Coworking dashboard</Nav.Link>
        </Nav.Item>
      ) : null}
      {canViewDetails ? (
        <Nav.Item>
          <Nav.Link as={Link} href={`/coworkings/${coworkingId}`} active={pathname === `/coworkings/${coworkingId}`}>Coworking details</Nav.Link>
        </Nav.Item>
      ) : null}
      {canViewPlaces ? (
        <Nav.Item>
          <Nav.Link as={Link} href={`/coworkings/${coworkingId}/places`} active={pathname === `/coworkings/${coworkingId}/places`}>Places</Nav.Link>
        </Nav.Item>
      ) : null}
      {canViewAccess ? (
        <Nav.Item>
          <Nav.Link as={Link} href={`/coworkings/${coworkingId}/staff`} active={pathname === `/coworkings/${coworkingId}/staff`}>Access</Nav.Link>
        </Nav.Item>
      ) : null}
    </Nav>
  );
}
