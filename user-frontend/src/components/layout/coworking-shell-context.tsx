'use client';

import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { usePathname } from 'next/navigation';
import { requestJson } from '@/lib/client/api';
import type { CoworkingShellContext, UserProfile } from '@/lib/types';

type ShellContextValue = {
  coworkingId: number | null;
  context: CoworkingShellContext | null;
  profile: UserProfile | null;
  loading: boolean;
  refreshContext: () => Promise<void>;
};

const ShellContext = createContext<ShellContextValue | null>(null);

function extractCoworkingId(pathname: string): number | null {
  const match = pathname.match(/^\/coworkings\/(\d+)/);
  if (!match) return null;
  const parsed = Number(match[1]);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

export function useCoworkingShellContext(): ShellContextValue {
  const value = useContext(ShellContext);
  if (!value) {
    throw new Error('useCoworkingShellContext must be used inside CoworkingShellProvider.');
  }
  return value;
}

export function CoworkingShellProvider({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const coworkingId = useMemo(() => extractCoworkingId(pathname), [pathname]);
  const [context, setContext] = useState<CoworkingShellContext | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(false);

  const refreshContext = useCallback(async () => {
    if (pathname === '/login' || pathname === '/register') {
      setContext(null);
      setProfile(null);
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      if (coworkingId) {
        const data = await requestJson<CoworkingShellContext>(`/api/coworkings/${coworkingId}/me/context`);
        setContext(data);
        setProfile(data.user);
        return;
      }

      setContext(null);
      const data = await requestJson<UserProfile>('/api/users/me');
      setProfile(data);
    } catch {
      setContext(null);
      setProfile(null);
    } finally {
      setLoading(false);
    }
  }, [coworkingId, pathname]);

  useEffect(() => {
    void refreshContext();
  }, [refreshContext]);

  useEffect(() => {
    const handleRefresh = () => {
      void refreshContext();
    };

    window.addEventListener('coworking-context-refresh', handleRefresh);
    return () => {
      window.removeEventListener('coworking-context-refresh', handleRefresh);
    };
  }, [refreshContext]);

  const value = useMemo<ShellContextValue>(() => ({
    coworkingId,
    context,
    profile,
    loading,
    refreshContext,
  }), [context, coworkingId, loading, profile, refreshContext]);

  return <ShellContext.Provider value={value}>{children}</ShellContext.Provider>;
}

export function notifyCoworkingContextChanged(): void {
  window.dispatchEvent(new Event('coworking-context-refresh'));
}
