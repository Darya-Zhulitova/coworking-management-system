import { AppHeader } from "./app-header";

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div>
      <AppHeader />
      <main>{children}</main>
    </div>
  );
}
