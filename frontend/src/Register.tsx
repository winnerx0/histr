import { useState, type FormEvent } from "react";
import { useAuth } from "./auth";

type Props = {
  onSwitch: () => void;
  onBack: () => void;
};

export function Register({ onSwitch, onBack }: Props) {
  const { register } = useAuth();
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await register({ username, email, password });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed");
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
          <h1>Create your account</h1>
          <p className="subtitle">
            Start tracking your transactions in minutes.
          </p>
        </div>

        {error && <p className="auth-error">{error}</p>}

        <div className="field">
          <label htmlFor="register-username">Username</label>
          <input
            id="register-username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
          />
        </div>

        <div className="field">
          <label htmlFor="register-email">Email</label>
          <input
            id="register-email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            required
          />
        </div>

        <div className="field">
          <label htmlFor="register-password">Password</label>
          <input
            id="register-password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
            minLength={6}
            required
          />
        </div>

        <button type="submit" className="primary-btn" disabled={submitting}>
          {submitting ? "Creating account…" : "Create account"}
        </button>

        <p className="auth-switch">
          Already have an account?{" "}
          <button type="button" onClick={onSwitch}>
            Sign in
          </button>
        </p>
      </form>
    </div>
  );
}
