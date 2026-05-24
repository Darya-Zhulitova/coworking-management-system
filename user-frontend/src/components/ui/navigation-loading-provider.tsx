'use client';

import { ReactNode, useCallback, useEffect, useRef, useState } from 'react';
import { usePathname } from 'next/navigation';
import { LoadingOverlay } from '@/components/ui/loading-overlay';

type NavigationLoadingProviderProps = {
  children: ReactNode;
};

const SHOW_DELAY_MS = 250;
const MAX_VISIBLE_MS = 10000;

function isModifiedClick(event: MouseEvent): boolean {
  return event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0;
}

function findAnchor(target: EventTarget | null): HTMLAnchorElement | null {
  if (!(target instanceof Element)) {
    return null;
  }

  return target.closest('a[href]');
}

function shouldTrackNavigation(anchor: HTMLAnchorElement): boolean {
  if (anchor.target && anchor.target !== '_self') {
    return false;
  }

  if (anchor.hasAttribute('download')) {
    return false;
  }

  const nextUrl = new URL(anchor.href, window.location.href);
  const currentUrl = new URL(window.location.href);

  if (nextUrl.origin !== currentUrl.origin) {
    return false;
  }

  const isSamePath = nextUrl.pathname === currentUrl.pathname;
  const isSameSearch = nextUrl.search === currentUrl.search;
  const isOnlyHashChange = isSamePath && isSameSearch && nextUrl.hash !== currentUrl.hash;

  return !isOnlyHashChange && nextUrl.href !== currentUrl.href;
}

export function NavigationLoadingProvider({ children }: NavigationLoadingProviderProps) {
  const pathname = usePathname();
  const [isVisible, setIsVisible] = useState(false);
  const showTimerRef = useRef<number | null>(null);
  const maxTimerRef = useRef<number | null>(null);

  const clearTimers = useCallback(() => {
    if (showTimerRef.current !== null) {
      window.clearTimeout(showTimerRef.current);
      showTimerRef.current = null;
    }

    if (maxTimerRef.current !== null) {
      window.clearTimeout(maxTimerRef.current);
      maxTimerRef.current = null;
    }
  }, []);

  const startNavigationLoading = useCallback(() => {
    clearTimers();

    showTimerRef.current = window.setTimeout(() => {
      setIsVisible(true);
      maxTimerRef.current = window.setTimeout(() => {
        setIsVisible(false);
        maxTimerRef.current = null;
      }, MAX_VISIBLE_MS);
    }, SHOW_DELAY_MS);
  }, [clearTimers]);

  const stopNavigationLoading = useCallback(() => {
    clearTimers();
    setIsVisible(false);
  }, [clearTimers]);

  useEffect(() => {
    const handleClick = (event: MouseEvent) => {
      if (event.defaultPrevented || isModifiedClick(event)) {
        return;
      }

      const anchor = findAnchor(event.target);
      if (!anchor || !shouldTrackNavigation(anchor)) {
        return;
      }

      startNavigationLoading();
    };

    const handlePopState = () => {
      startNavigationLoading();
    };

    document.addEventListener('click', handleClick, true);
    window.addEventListener('popstate', handlePopState);

    return () => {
      document.removeEventListener('click', handleClick, true);
      window.removeEventListener('popstate', handlePopState);
      clearTimers();
    };
  }, [clearTimers, startNavigationLoading]);

  useEffect(() => {
    stopNavigationLoading();
  }, [pathname, stopNavigationLoading]);

  return (
    <>
      {children}
      {isVisible ? <LoadingOverlay message="Загрузка..." ariaLabel="Загрузка"/> : null}
    </>
  );
}
