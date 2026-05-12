export type Transaction = {
  id: string;
  amount: number;
  recipient: string | null;
  description: string;
  category: string;
  createdAt: string;
};

export type TransactionsResponse = {
  data: Transaction[];
  pagination: { limit: number; pageNo: number; total: number };
};

export type StatsResponse = {
  totalIncome: number;
  totalExpense: number;
  netTotal: number;
  transactionCount: number;
};

export type CategorySummaryResponse = {
  data: { category: string; total: number; count: number }[];
};

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
};

export type RegisterPayload = {
  username: string;
  email: string;
  password: string;
};

export type LoginPayload = {
  username: string;
  password: string;
};

const apiBase =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:80/api/v1";
const apiOrigin = apiBase.replace(/\/api\/v1\/?$/, "");
export const googleOAuthUrl =
  import.meta.env.VITE_GOOGLE_OAUTH_URL ?? `${apiOrigin}/oauth/login/google`;

const ACCESS_KEY = "histr.accessToken";
const REFRESH_KEY = "histr.refreshToken";

export const getAccessToken = () => localStorage.getItem(ACCESS_KEY);
export const setAccessToken = (token: string | null) => {
  if (token) localStorage.setItem(ACCESS_KEY, token);
  else localStorage.removeItem(ACCESS_KEY);
};

export const getRefreshToken = () => localStorage.getItem(REFRESH_KEY);
export const setRefreshToken = (token: string | null) => {
  if (token) localStorage.setItem(REFRESH_KEY, token);
  else localStorage.removeItem(REFRESH_KEY);
};

export const clearTokens = () => {
  setAccessToken(null);
  setRefreshToken(null);
};

// Called when refresh fails — lets the app react (logout, redirect, etc.)
let onAuthFailure: (() => void) | null = null;
export const setOnAuthFailure = (cb: (() => void) | null) => {
  onAuthFailure = cb;
};

// Called after a successful silent refresh, so React state can re-sync.
let onTokenRefreshed: ((accessToken: string) => void) | null = null;
export const setOnTokenRefreshed = (cb: ((t: string) => void) | null) => {
  onTokenRefreshed = cb;
};

// Decode a JWT's exp (seconds since epoch). Returns null if unparseable.
export const getTokenExpiry = (token: string): number | null => {
  try {
    const [, payload] = token.split(".");
    if (!payload) return null;
    const json = JSON.parse(
      atob(payload.replace(/-/g, "+").replace(/_/g, "/")),
    );
    return typeof json.exp === "number" ? json.exp : null;
  } catch {
    return null;
  }
};

// True if the token is missing, unparseable, or expires within `skewSeconds`.
export const isTokenExpiring = (token: string | null, skewSeconds = 30) => {
  if (!token) return true;
  const exp = getTokenExpiry(token);
  if (exp === null) return true;
  return Date.now() / 1000 >= exp - skewSeconds;
};

const asQuery = (params: Record<string, string | number | undefined>) => {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") query.set(key, String(value));
  }
  const serialized = query.toString();
  return serialized ? `?${serialized}` : "";
};

const parseError = async (response: Response, fallback: string) => {
  try {
    const payload = await response.json();
    return payload?.message ?? fallback;
  } catch {
    return fallback;
  }
};

// ----- token refresh (single-flight) -----
let refreshInFlight: Promise<string | null> | null = null;

export const refreshAccessToken = async (): Promise<string | null> => {
  if (refreshInFlight) return refreshInFlight;

  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;

  refreshInFlight = (async () => {
    try {
      const response = await fetch(`${apiBase}/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
      });
      if (!response.ok) {
        clearTokens();
        onAuthFailure?.();
        return null;
      }
      const payload = (await response.json()) as AuthResponse;
      setAccessToken(payload.accessToken);
      setRefreshToken(payload.refreshToken);
      onTokenRefreshed?.(payload.accessToken);
      return payload.accessToken;
    } catch {
      clearTokens();
      onAuthFailure?.();
      return null;
    } finally {
      refreshInFlight = null;
    }
  })();

  return refreshInFlight;
};

const sendWith = (path: string, init: RequestInit, token: string | null) => {
  const headers = new Headers(init.headers);
  if (token) headers.set("Authorization", `Bearer ${token}`);
  // IMPORTANT: do not set Content-Type when body is FormData — the browser
  // must set it itself to include the multipart boundary.
  if (init.body instanceof FormData) headers.delete("Content-Type");
  return fetch(`${apiBase}${path}`, { ...init, headers });
};

const authedFetch = async (path: string, init: RequestInit = {}) => {
  const token = getAccessToken();
  const response = await sendWith(path, init, token);

  if (response.status !== 401) return response;

  const newToken = await refreshAccessToken();
  if (!newToken) return response;

  return sendWith(path, init, newToken);
};

// ----- auth -----
export const registerUser = async (payload: RegisterPayload) => {
  const response = await fetch(`${apiBase}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw new Error(await parseError(response, "Registration failed"));
  }
  return (await response.json()) as AuthResponse;
};

export const loginUser = async (payload: LoginPayload) => {
  const response = await fetch(`${apiBase}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw new Error(await parseError(response, "Invalid credentials"));
  }
  return (await response.json()) as AuthResponse;
};

// ----- data -----
export const fetchTransactions = async (params: {
  limit: number;
  pageNo: number;
  search: string;
  startDate?: string;
  endDate?: string;
}) => {
  const response = await authedFetch(
    `/transactions${asQuery({
      limit: params.limit,
      pageNo: params.pageNo,
      search: params.search,
      startDate: params.startDate,
      endDate: params.endDate,
    })}`,
  );
  if (!response.ok) throw new Error("Unable to load transactions");
  return (await response.json()) as TransactionsResponse;
};

export const fetchStats = async () => {
  const response = await authedFetch(`/transactions/stats`);
  if (!response.ok) throw new Error("Unable to load dashboard stats");
  return (await response.json()) as StatsResponse;
};

export const fetchCategorySummary = async () => {
  const response = await authedFetch(`/categories/summary`);
  if (!response.ok) throw new Error("Unable to load category summary");
  return (await response.json()) as CategorySummaryResponse;
};

export const parseLocalDocuments = async () => {
  const response = await authedFetch(`/parse`);
  const payload = await response.json();
  if (!response.ok) throw new Error(payload.message ?? "Parse failed");
  return payload as { message: string; queuedBatches: number };
};

export const uploadTransactionDocument = async (file: File) => {
  const formData = new FormData();
  formData.append("file", file);

  const response = await authedFetch(`/documents/upload`, {
    method: "POST",
    body: formData,
  });

  const payload = await response.json();
  if (!response.ok) throw new Error(payload.message ?? "Upload failed");
  return payload as {
    message: string;
    fileName: string;
    queuedBatches: number;
  };
};
