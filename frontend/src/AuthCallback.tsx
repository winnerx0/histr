import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "./auth";

export function AuthCallback() {
  const { completeOAuthLogin } = useAuth();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const accessToken = searchParams.get("accessToken");
    const refreshToken = searchParams.get("refreshToken");

    if (!accessToken || !refreshToken) {
      setError("Google sign in did not return valid tokens.");
      return;
    }

    completeOAuthLogin({ accessToken, refreshToken });
    navigate("/dashboard", { replace: true });
  }, [completeOAuthLogin, navigate, searchParams]);

  return (
    <div className="auth-page" aria-busy={!error}>
      <div className="auth-card">
        <div className="auth-brand">
          <span className="brand-mark">H</span>
          <span className="brand-name">Histr</span>
        </div>

        {error ? (
          <>
            <h1>Sign in failed</h1>
            <p className="auth-error">{error}</p>
          </>
        ) : (
          <>
            <h1>Signing you in</h1>
            <p className="subtitle">Finishing your Google sign in.</p>
          </>
        )}
      </div>
    </div>
  );
}
