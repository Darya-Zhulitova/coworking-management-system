import {ReactNode} from "react";
import {AppHeader} from "./app-header";

type AppShellProps = {
  children: ReactNode;
};

export function AppShell({children}: AppShellProps) {
  return (
    <div className="min-vh-100 bg-body-secondary">
      <AppHeader/>
      <main className="container py-4">{children}</main>
    </div>
  );
}
