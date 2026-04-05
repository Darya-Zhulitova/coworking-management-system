'use client';

import Nav from 'react-bootstrap/Nav';
import { usePathname } from 'next/navigation';
import Link from 'next/link';

export function TenantNav({ coworkingId }: { coworkingId: number }) {
  const pathname = usePathname();

  return (
    <Nav variant="tabs" className="tenant-nav">
      <Nav.Item>
        <Nav.Link as={Link} href="/coworkings" active={pathname === '/coworkings'}>All coworkings</Nav.Link>
      </Nav.Item>
      <Nav.Item>
        <Nav.Link as={Link} href={`/coworkings/${coworkingId}/dashboard`} active={pathname === `/coworkings/${coworkingId}/dashboard`}>Tenant dashboard</Nav.Link>
      </Nav.Item>
      <Nav.Item>
        <Nav.Link as={Link} href={`/coworkings/${coworkingId}`} active={pathname === `/coworkings/${coworkingId}`}>Tenant details</Nav.Link>
      </Nav.Item>
      <Nav.Item>
        <Nav.Link as={Link} href={`/coworkings/${coworkingId}/staff`} active={pathname === `/coworkings/${coworkingId}/staff`}>Staff access</Nav.Link>
      </Nav.Item>
    </Nav>
  );
}
