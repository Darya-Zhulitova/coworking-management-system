'use client';

import { useRouter } from 'next/navigation';
import Button from 'react-bootstrap/Button';

export function LogoutButton() {
  const router = useRouter();
  async function handleLogout() {
    await fetch('/api/auth/logout', { method: 'POST' });
    router.replace('/login');
    router.refresh();
  }
  return <Button variant="outline-secondary" onClick={handleLogout} type="button">Logout</Button>;
}
