'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import type { AdminSessionDto } from '@/types/session';
import { ClientRequestError, requestJson } from '@/lib/client/api';

interface UseAdminSessionOptions {
  redirectToLogin?: boolean;
}

export function useAdminSession(options: UseAdminSessionOptions = {}) {
  const router = useRouter();
  const [session, setSession] = useState<AdminSessionDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    requestJson<AdminSessionDto>('/api/session')
      .then((data) => {
        if (!isMounted) return;
        setSession(data);
      })
      .catch((error) => {
        if (!isMounted) return;
        if (error instanceof ClientRequestError && error.status === 401 && options.redirectToLogin) {
          router.replace('/login');
          return;
        }
        setErrorMessage(error instanceof Error ? error.message : 'Unable to load session.');
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [options.redirectToLogin, router]);

  return { session, isLoading, errorMessage };
}
