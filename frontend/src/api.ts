export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

export async function api<T>(path: string, body?: object | URLSearchParams): Promise<T> {
  const headers = new Headers({ Accept: 'application/json' });
  let payload: BodyInit | undefined;
  if (body !== undefined) {
    const csrf = await api<{headerName: string; token: string}>('/auth/csrf');
    headers.set(csrf.headerName, csrf.token);
    if (body instanceof URLSearchParams) {
      payload = body;
    } else {
      headers.set('Content-Type', 'application/json');
      payload = JSON.stringify(body);
    }
  }
  const response = await fetch('/api' + path, {
    credentials: 'same-origin',
    headers,
    method: body === undefined ? 'GET' : 'POST',
    body: payload,
  });
  if (!response.ok) {
    const problem = await response.json().catch(() => null);
    const message = problem?.errors?.join(' · ') || problem?.detail ||
      ({401: 'Usuário ou senha inválidos, ou sessão expirada.', 403: 'Você não tem permissão para esta operação.'} as Record<number, string>)[response.status] ||
      'A operação falhou (' + response.status + ').';
    throw new ApiError(response.status, message);
  }
  return response.status === 204 ? undefined as T : response.json();
}
