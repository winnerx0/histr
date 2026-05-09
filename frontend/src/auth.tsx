import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  isTokenExpiring,
  loginUser,
  refreshAccessToken,
  registerUser,
  setAccessToken,
  setOnAuthFailure,
  setOnTokenRefreshed,
  setRefreshToken,
  type LoginPayload,
  type RegisterPayload,
} from "./api";

type AuthContextValue = {
  token: string | null;
  username: string | null;
  bootstrapping: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  completeOAuthLogin: (tokens: {
    accessToken: string;
    refreshToken: string;
  }) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);
const USERNAME_KEY = "histr.username";

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [token, setToken] = useState<string | null>(() => getAccessToken());
  const [username, setUsername] = useState<string | null>(() =>
    localStorage.getItem(USERNAME_KEY),
  );
  // While we proactively refresh on startup, hold the UI to avoid a 401 flash.
  const [bootstrapping, setBootstrapping] = useState<boolean>(
    () => window.location.pathname !== "/callback" && !!getRefreshToken(),
  );

  // Persist username when it changes.
  useEffect(() => {
    if (username) localStorage.setItem(USERNAME_KEY, username);
    else localStorage.removeItem(USERNAME_KEY);
  }, [username]);

  // Wire the api layer's callbacks into React state.
  useEffect(() => {
    setOnAuthFailure(() => {
      setToken(null);
      setUsername(null);
    });
    setOnTokenRefreshed((newAccess) => {
      setToken(newAccess);
    });
    return () => {
      setOnAuthFailure(null);
      setOnTokenRefreshed(null);
    };
  }, []);

  // Startup: if access token is missing/expiring but we have a refresh token,
  // get a fresh pair before unlocking the UI.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (window.location.pathname === "/callback") {
        if (cancelled) return;
        setBootstrapping(false);
        return;
      }

      const access = getAccessToken();
      const refresh = getRefreshToken();

      if (!refresh) {
        if (cancelled) return;
        setBootstrapping(false);
        return;
      }

      if (!isTokenExpiring(access)) {
        if (cancelled) return;
        setBootstrapping(false);
        return;
      }

      const fresh = await refreshAccessToken();
      if (cancelled) return;
      if (fresh) {
        setToken(fresh);
      } else {
        setToken(null);
        setUsername(null);
      }
      setBootstrapping(false);
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  // Sync across tabs.
  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === "histr.accessToken") setToken(getAccessToken());
      if (e.key === USERNAME_KEY)
        setUsername(localStorage.getItem(USERNAME_KEY));
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      username,
      bootstrapping,
      login: async (payload) => {
        const result = await loginUser(payload);
        setAccessToken(result.accessToken);
        setRefreshToken(result.refreshToken);
        setToken(result.accessToken);
        setUsername(payload.username);
      },
      register: async (payload) => {
        const result = await registerUser(payload);
        setAccessToken(result.accessToken);
        setRefreshToken(result.refreshToken);
        setToken(result.accessToken);
        setUsername(payload.username);
      },
      completeOAuthLogin: ({ accessToken, refreshToken }) => {
        setAccessToken(accessToken);
        setRefreshToken(refreshToken);
        setToken(accessToken);
        setUsername(null);
      },
      logout: () => {
        clearTokens();
        setToken(null);
        setUsername(null);
      },
    }),
    [token, username, bootstrapping],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
};
