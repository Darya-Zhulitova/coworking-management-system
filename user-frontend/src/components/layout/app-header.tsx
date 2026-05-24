'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { usePathname } from 'next/navigation';
import { formatMoney } from '@/lib/format';
import { ThemeToggleButton } from '@/components/theme/theme-toggle-button';
import { useCoworkingShellContext } from '@/components/layout/coworking-shell-context';

type NavItem = {
  key: string;
  href: string;
  label: string;
  exact?: boolean;
};

function extractMembershipId(pathname: string): string | null {
  const match = pathname.match(/^\/memberships\/(\d+)/);
  return match ? match[1] : null;
}

function isActive(pathname: string, item: NavItem): boolean {
  return item.exact ? pathname === item.href : pathname === item.href || pathname.startsWith(`${item.href}/`);
}

function activeNavClass(pathname: string, item: NavItem): string {
  return `nav-link px-2 text-body-emphasis text-decoration-none ${isActive(pathname, item) ? 'active fw-semibold text-decoration-underline' : ''}`.trim();
}

export function AppHeader() {
  const pathname = usePathname();
  const membershipId = extractMembershipId(pathname);
  const isAuthPage = pathname === '/login' || pathname === '/register';
  const { context, profile } = useCoworkingShellContext();
  const coworking = context?.coworking ?? null;
  const [isNavbarOpen, setIsNavbarOpen] = useState(false);

  useEffect(() => {
    setIsNavbarOpen(false);
  }, [pathname]);

  const navItems: NavItem[] = useMemo(() => {
    if (membershipId) {
      return [
        { key: 'tenant-home', href: `/memberships/${membershipId}`, label: 'Коворкинг', exact: true },
        { key: 'tenant-bookings', href: `/memberships/${membershipId}/bookings`, label: 'Бронирования' },
        { key: 'tenant-balance', href: `/memberships/${membershipId}/balance`, label: 'Баланс' },
        { key: 'tenant-requests', href: `/memberships/${membershipId}/requests`, label: 'Сервисные заявки' },
        { key: 'tenant-profile', href: `/memberships/${membershipId}/profile`, label: 'Профиль', exact: true },
      ];
    }

    return [
      { key: 'global-home', href: '/', label: 'Мои коворкинги', exact: true },
      { key: 'global-profile', href: '/profile', label: 'Профиль', exact: true },
    ];
  }, [membershipId]);

  const coworkingLabel = membershipId ? (coworking?.name ?? 'Загрузка коворкинга...') : null;
  const userName = profile?.name ?? 'Пользователь';
  const balanceLabel = membershipId && context ? formatMoney(context.membership.balanceMinorUnits) : null;
  const coworkingHomeHref = membershipId ? `/memberships/${membershipId}` : null;
  const balanceHref = membershipId ? `/memberships/${membershipId}/balance` : null;
  const profileHref = membershipId ? `/memberships/${membershipId}/profile` : '/profile';

  if (isAuthPage) {
    return null;
  }

  return (
    <header className="bg-primary sticky-top shadow-sm" data-bs-theme="dark">
      <div className="container">
        <nav className="navbar navbar-expand-lg px-0 py-3" aria-label="Основная навигация пользователя">
          <div className="d-flex align-items-center flex-grow-1 min-w-0 gap-3">
            <Link href="/" className="navbar-brand fw-semibold me-0 d-inline-flex align-items-center gap-0">
              <Image src="/brand-icon.svg" alt="" width={30} height={30} priority aria-hidden="true"/>
              <span>SpaceBooking</span>
            </Link>
            {coworkingLabel && coworkingHomeHref ? (
              <a className="navbar-brand fw-semibold me-0">|</a>
            ) : null}
            {coworkingLabel && coworkingHomeHref ? (
              <Link
                href={coworkingHomeHref}
                className="navbar-brand fw-semibold me-0"
                title={coworkingLabel}
              >
                {coworkingLabel}
              </Link>
            ) : null}
          </div>

          <button
            className="navbar-toggler"
            type="button"
            aria-controls="userNavbar"
            aria-expanded={isNavbarOpen}
            aria-label="Переключить навигацию"
            onClick={() => setIsNavbarOpen((current) => !current)}
          >
            <span className="navbar-toggler-icon"/>
          </button>

          <div className={`collapse navbar-collapse justify-content-end ${isNavbarOpen ? 'show' : ''}`} id="userNavbar">
            <div className="d-flex flex-column flex-lg-row align-items-lg-center gap-3 gap-lg-4 pt-3 pt-lg-0">
              <ul className="navbar-nav align-items-lg-center gap-lg-2">
                {navItems.map((item) => (
                  <li className="nav-item" key={item.key}>
                    <Link href={item.href} className={activeNavClass(pathname, item)}>
                      {item.label}
                    </Link>
                  </li>
                ))}
              </ul>

              <div className="d-flex flex-column flex-lg-row align-items-lg-center gap-3 gap-lg-2 small">
                <Link href={profileHref} className="text-body-emphasis text-decoration-none fw-medium">
                  {userName}
                </Link>
                {balanceLabel && balanceHref ? (
                  <Link href={balanceHref} className="text-body-emphasis text-decoration-none fw-medium">
                    {balanceLabel}
                  </Link>
                ) : null}
                <ThemeToggleButton/>
              </div>
            </div>
          </div>
        </nav>
      </div>
    </header>
  );
}
