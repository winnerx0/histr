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
  profilePicture: string | null;
  bootstrapping: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  completeOAuthLogin: (tokens: {
    accessToken: string;
    refreshToken: string;
    username?: string | null;
    profilePicture?: string | null;
  }) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);
const USERNAME_KEY = "histr.username";
const PROFILE_PICTURE_KEY = "histr.profilePicture";

const decodeJwtPayload = (token: string): Record<string, unknown> | null => {
  try {
    const [, payload] = token.split(".");
    if (!payload) return null;
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(
      normalized.length + ((4 - (normalized.length % 4)) % 4),
      "=",
    );
    return JSON.parse(atob(padded)) as Record<string, unknown>;
  } catch {
    return null;
  }
};

const stringClaim = (
  claims: Record<string, unknown> | null,
  keys: string[],
) => {
  if (!claims) return null;
  for (const key of keys) {
    const value = claims[key];
    if (typeof value === "string" && value.trim()) return value;
  }
  return null;
};

const profileFromToken = (token: string) => {
  const claims = decodeJwtPayload(token);
  return {
    username: stringClaim(claims, [
      "username",
      "name",
      "preferred_username",
      "email",
    ]),
    profilePicture: stringClaim(claims, [
      "picture",
      "profilePicture",
      "avatar",
      "avatar_url",
    ]),
  };
};

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [token, setToken] = useState<string | null>(() => getAccessToken());
  const [username, setUsername] = useState<string | null>(() =>
    localStorage.getItem(USERNAME_KEY),
  );
  const [profilePicture, setProfilePicture] = useState<string | null>(() =>
    localStorage.getItem(PROFILE_PICTURE_KEY),
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

  useEffect(() => {
    if (profilePicture)
      localStorage.setItem(PROFILE_PICTURE_KEY, profilePicture);
    else localStorage.removeItem(PROFILE_PICTURE_KEY);
  }, [profilePicture]);

  // Wire the api layer's callbacks into React state.
  useEffect(() => {
    setOnAuthFailure(() => {
      setToken(null);
      setUsername(null);
      setProfilePicture(null);
    });
    setOnTokenRefreshed((newAccess) => {
      setToken(newAccess);
      const profile = profileFromToken(newAccess);
      if (profile.username) setUsername(profile.username);
      if (profile.profilePicture) setProfilePicture(profile.profilePicture);
    });
    return () => {
      setOnAuthFailure(null);
      setOnTokenRefreshed(null);
    };
  }, []);

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
        const profile = profileFromToken(fresh);
        if (profile.username) setUsername(profile.username);
        if (profile.profilePicture) setProfilePicture(profile.profilePicture);
      } else {
        setToken(null);
        setUsername(null);
        setProfilePicture(null);
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
      if (e.key === PROFILE_PICTURE_KEY)
        setProfilePicture(localStorage.getItem(PROFILE_PICTURE_KEY));
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      username,
      profilePicture,
      bootstrapping,
      login: async (payload) => {
        const result = await loginUser(payload);
        setAccessToken(result.accessToken);
        setRefreshToken(result.refreshToken);
        setToken(result.accessToken);
        setUsername(payload.username);
        setProfilePicture(null);
      },
      register: async (payload) => {
        const result = await registerUser(payload);
        setAccessToken(result.accessToken);
        setRefreshToken(result.refreshToken);
        setToken(result.accessToken);
        setUsername(payload.username);
        setProfilePicture(null);
      },
      completeOAuthLogin: ({
        accessToken,
        refreshToken,
        username,
        profilePicture,
      }) => {
        const profile = profileFromToken(accessToken);
        setAccessToken(accessToken);
        setRefreshToken(refreshToken);
        setToken(accessToken);
        setUsername(username?.trim() || profile.username);
        setProfilePicture(profilePicture?.trim() || profile.profilePicture);
      },
      logout: () => {
        clearTokens();
        setToken(null);
        setUsername(null);
        setProfilePicture(null);
      },
    }),
    [token, username, profilePicture, bootstrapping],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
};
