// Minimal auth token store. The access token lives in memory only (not
// localStorage) to reduce XSS exfiltration risk; the refresh token is an
// httpOnly cookie set by the backend, never touched from JS. This gets
// wired into a proper AuthContext + login/logout flow in Phase 6.

let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function clearAuth(): void {
  accessToken = null;
}
