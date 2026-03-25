import { ReactNode } from "react";
import { AppHeader } from "./app-header";

type AppShellProps = {
  children: ReactNode;
};

export function AppShell({ children }: AppShellProps) {
  return (
    <div>
      <AppHeader />
      <main className="page">{children}</main>
    </div>
  );
}
