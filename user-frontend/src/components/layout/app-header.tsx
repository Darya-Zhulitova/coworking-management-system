'use client';

import Link from 'next/link';
import {usePathname} from 'next/navigation';
import {currentUser, getCoworking} from '@/lib/mock-data';

type NavItem = {
  key: string;
  href: string;
  label: string;
  icon: string;
  exact?: boolean;
};

function extractCoworkingId(pathname: string): string | null {
  const match = pathname.match(/^\/coworkings\/(\d+)/);
  return match ? match[1] : null;
}

function navClass(active: boolean): string {
  return active ? 'nav-link active fw-semibold' : 'nav-link';
}

function isActive(pathname: string, item: NavItem): boolean {
  return item.exact ? pathname === item.href : pathname === item.href || pathname.startsWith(`${item.href}/`);
}

export function AppHeader() {
  const pathname = usePathname();
  const coworkingId = extractCoworkingId(pathname);
  const coworking = coworkingId ? getCoworking(Number(coworkingId)) : undefined;
  const profileHref = coworkingId ? `/coworkings/${coworkingId}/profile` : '/profile';

  const navItems: NavItem[] = coworkingId
    ? [
        {key: 'tenant-home', href: `/coworkings/${coworkingId}`, label: 'Коворкинг', icon: '🏠', exact: true},
        {key: 'tenant-bookings', href: `/coworkings/${coworkingId}/bookings`, label: 'Бронирования', icon: '📅'},
        {key: 'tenant-balance', href: `/coworkings/${coworkingId}/balance`, label: 'Баланс', icon: '💳'},
        {key: 'tenant-requests', href: `/coworkings/${coworkingId}/requests`, label: 'Заявки', icon: '🛠️'},
        {key: 'tenant-profile', href: profileHref, label: 'Профиль', icon: '👤'},
      ]
    : [
        {key: 'global-home', href: '/', label: 'Коворкинг', icon: '🏢', exact: true},
        {key: 'global-profile', href: '/profile', label: 'Профиль', icon: '👤'},
      ];

  return (
    <>
      <header className="bg-white border-bottom sticky-top header-surface">
        <div className="container py-3">
          <div className="d-flex flex-column gap-3">
            <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
              <div>
                <Link href={coworkingId ? `/coworkings/${coworkingId}` : '/'} className="navbar-brand fw-bold mb-0">
                  Coworking Resident
                </Link>
                <div className="text-body-secondary small">
                  {coworking ? coworking.name : 'Личный кабинет пользователя'}
                </div>
              </div>
              <div className="d-flex align-items-center gap-2">
                <div className="text-end d-none d-sm-block">
                  <div className="fw-semibold">{currentUser.name}</div>
                  <div className="text-body-secondary small">{currentUser.email}</div>
                </div>
                <Link href={profileHref} className="btn btn-outline-secondary rounded-pill px-3">
                  {currentUser.avatarLabel}
                </Link>
              </div>
            </div>
            <nav className="nav nav-pills flex-nowrap overflow-auto d-none d-md-flex">
              {navItems.map((item) => {
                const active = isActive(pathname, item);
                return (
                  <Link key={item.key} href={item.href} className={navClass(active)}>
                    {item.label}
                  </Link>
                );
              })}
            </nav>
          </div>
        </div>
      </header>

      <nav className="mobile-bottom-nav d-md-none bg-white border-top">
        <div className="container-fluid px-2">
          <div className={`row row-cols-${navItems.length} g-0`}>
            {navItems.map((item) => {
              const active = isActive(pathname, item);
              return (
                <div className="col" key={item.key}>
                  <Link href={item.href} className={`mobile-bottom-link ${active ? 'active' : ''}`}>
                    <span className="mobile-bottom-icon" aria-hidden="true">{item.icon}</span>
                    <span className="mobile-bottom-label">{item.label}</span>
                  </Link>
                </div>
              );
            })}
          </div>
        </div>
      </nav>
    </>
  );
}
