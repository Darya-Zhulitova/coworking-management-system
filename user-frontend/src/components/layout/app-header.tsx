import Link from "next/link";

export function AppHeader() {
  return (
    <header className="header">
      <div className="header-inner">
        <nav>
          <Link href="/">Home</Link> |
          <Link href="/login">Login</Link> |
          <Link href="/bookings">Bookings</Link> |
          <Link href="/balance">Balance</Link> |
          <Link href="/requests">Requests</Link> |
          <Link href="/memberships">Memberships</Link>
        </nav>
      </div>
    </header>
  );
}
