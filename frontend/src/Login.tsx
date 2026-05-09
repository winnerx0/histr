import { useState, type FormEvent } from "react";
import { FcGoogle } from "react-icons/fc";
import { googleOAuthUrl } from "./api";
import { useAuth } from "./auth";

type Props = {
  onSwitch: () => void;
  onBack: () => void;
};

export function Login({ onSwitch, onBack }: Props) {
  const { login } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login({ username, password });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <button type="button" className="auth-back" onClick={onBack}>
        ← Back to home
      </button>

      <form className="auth-card" onSubmit={handleSubmit}>
        <div className="auth-brand">
          <span className="brand-mark">H</span>
          <span className="brand-name">Histr</span>
        </div>

        <div>
          <h1>Welcome back</h1>
          <p className="subtitle">Sign in to your Histr account.</p>
        </div>

        {error && <p className="auth-error">{error}</p>}

        <a className="oauth-btn" href={googleOAuthUrl}>
          <FcGoogle aria-hidden="true" />
          <span>Continue with Google</span>
        </a>

        <div className="auth-divider">
          <span>or</span>
        </div>

        <div className="field">
          <label htmlFor="login-username">Username</label>
          <input
            id="login-username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
          />
        </div>

        <div className="field">
          <label htmlFor="login-password">Password</label>
          <input
            id="login-password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </div>

        <button type="submit" className="primary-btn" disabled={submitting}>
          {submitting ? "Signing in…" : "Sign in"}
        </button>

        <p className="auth-switch">
          Don't have an account?{" "}
          <button type="button" onClick={onSwitch}>
            Create one
          </button>
        </p>
      </form>
    </div>
  );
}
