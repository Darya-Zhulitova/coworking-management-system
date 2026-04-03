import Link from "next/link";

const navLinks = [
  {href: "/", label: "Home"},
  {href: "/login", label: "Login"},
  {href: "/bookings", label: "Bookings"},
  {href: "/balance", label: "Balance"},
  {href: "/requests", label: "Requests"},
  {href: "/memberships", label: "Memberships"},
];

export function AppHeader() {
  return (
    <header className="border-bottom bg-body-tertiary">
      <div className="container py-3">
        <div className="d-flex flex-column gap-3">
          <div>
            <h1 className="h4 mb-1">User Frontend</h1>
            <p className="text-body-secondary mb-0">Пользовательский контур коворкинга</p>
          </div>

          <nav className="nav nav-pills flex-wrap gap-2">
            {navLinks.map((link) => (
              <Link key={link.href} href={link.href} className="nav-link px-3 py-2 border rounded-pill">
                {link.label}
              </Link>
            ))}
          </nav>
        </div>
      </div>
    </header>
  );
}
