'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import type { AppContextDto } from '@/types/context';
import { ClientRequestError, requestJson } from '@/lib/client/api';

interface UseAppContextOptions {
  coworkingId?: number;
  redirectToLogin?: boolean;
}

export function useAppContext(options: UseAppContextOptions = {}) {
  const router = useRouter();
  const [context, setContext] = useState<AppContextDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    const query = options.coworkingId == null ? '' : `?coworkingId=${options.coworkingId}`;

    requestJson<AppContextDto>(`/api/context${query}`)
      .then((data) => {
        if (!isMounted) return;
        setContext(data);
      })
      .catch((error) => {
        if (!isMounted) return;
        if (error instanceof ClientRequestError && error.status === 401 && options.redirectToLogin) {
          router.replace('/login');
          return;
        }
        setErrorMessage(error instanceof Error ? error.message : 'Unable to load context.');
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [options.coworkingId, options.redirectToLogin, router]);

  return { context, isLoading, errorMessage };
}
