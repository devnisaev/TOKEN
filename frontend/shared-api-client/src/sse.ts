export interface SseMessage<T = unknown> {
  event?: string;
  data: T;
}

export interface SubscribeSseOptions {
  baseUrl: string;
  path: string;
  getAccessToken?: () => string | null;
  signal?: AbortSignal;
}

export async function* subscribeSse<T>(
  options: SubscribeSseOptions,
): AsyncGenerator<SseMessage<T>> {
  const headers: Record<string, string> = { Accept: 'text/event-stream' };
  const token = options.getAccessToken?.();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${options.baseUrl}${options.path}`, {
    headers,
    signal: options.signal,
  });

  if (!response.ok) {
    let detail = response.statusText;
    try {
      const body = (await response.json()) as { detail?: string; title?: string };
      detail = body.detail ?? body.title ?? detail;
    } catch {
      /* empty body */
    }
    throw new Error(detail);
  }

  if (!response.body) {
    throw new Error('SSE response has no body');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true });
    const chunks = buffer.split('\n\n');
    buffer = chunks.pop() ?? '';
    for (const chunk of chunks) {
      const message = parseSseChunk<T>(chunk);
      if (message) {
        yield message;
      }
    }
  }
}

function parseSseChunk<T>(chunk: string): SseMessage<T> | null {
  const lines = chunk.split('\n');
  let event: string | undefined;
  const dataLines: string[] = [];

  for (const line of lines) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim());
    }
  }

  if (dataLines.length === 0) {
    return null;
  }

  const raw = dataLines.join('\n');
  let data: T;
  try {
    data = JSON.parse(raw) as T;
  } catch {
    data = raw as T;
  }

  return { event, data };
}
