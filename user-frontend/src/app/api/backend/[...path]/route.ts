import { env } from '@/lib/config/env';
import { getUserSession } from '@/lib/auth/session';

const CONTENT_HEADERS = new Set([
  'accept',
  'content-type',
]);

type ProxyContext = {
  params: Promise<{ path?: string[] }>;
};

function buildBackendUrl(request: Request, pathSegments: string[] = []): string {
  const incomingUrl = new URL(request.url);
  const backendPath = `/api/${pathSegments.map(encodeURIComponent).join('/')}`;
  return `${env.backendBaseUrl}${backendPath}${incomingUrl.search}`;
}

function buildForwardHeaders(request: Request, token: string): Headers {
  const headers = new Headers();

  request.headers.forEach((value, key) => {
    const normalizedKey = key.toLowerCase();
    if (CONTENT_HEADERS.has(normalizedKey)) {
      headers.set(key, value);
    }
  });

  headers.set('Accept', request.headers.get('accept') ?? 'application/json');
  headers.set('Authorization', `Bearer ${token}`);
  return headers;
}

async function proxyToBackend(request: Request, context: ProxyContext): Promise<Response> {
  const session = await getUserSession();
  if (!session) {
    return Response.json({ message: 'Необходимо войти в систему.' }, { status: 401 });
  }

  const { path = [] } = await context.params;
  const method = request.method.toUpperCase();
  const hasBody = method !== 'GET' && method !== 'HEAD';
  const backendResponse = await fetch(buildBackendUrl(request, path), {
    method,
    headers: buildForwardHeaders(request, session.token),
    body: hasBody ? await request.text() : undefined,
    cache: 'no-store',
  });

  const responseHeaders = new Headers();
  const contentType = backendResponse.headers.get('content-type');
  if (contentType) responseHeaders.set('content-type', contentType);

  return new Response(await backendResponse.arrayBuffer(), {
    status: backendResponse.status,
    statusText: backendResponse.statusText,
    headers: responseHeaders,
  });
}

export const GET = proxyToBackend;
export const POST = proxyToBackend;
export const PUT = proxyToBackend;
export const PATCH = proxyToBackend;
export const DELETE = proxyToBackend;
