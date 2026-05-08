type Props = {
  onSignIn: () => void;
  onGetStarted: () => void;
};

export function Landing({ onSignIn, onGetStarted }: Props) {
  return (
    <div className="landing">
      <header className="landing-nav">
        <div className="brand-block">
          <span className="brand-mark">H</span>
          <span className="brand-name">Histr</span>
        </div>
        <nav className="landing-nav-links">
          <a href="#features">Features</a>
          <a href="#how">How it works</a>
          <button className="secondary-btn" onClick={onSignIn}>
            Sign in
          </button>
          <button className="primary-btn" onClick={onGetStarted}>
            Get started
          </button>
        </nav>
      </header>

      <section className="landing-hero">
        <p className="eyebrow">Personal financial dashboard</p>
        <h1 className="landing-title">
          Make sense of every <em>naira</em> you spend.
        </h1>
        <p className="landing-lede">
          Upload your bank statements, let Histr categorize the noise, and see
          where your money actually goes — all in one quiet dashboard.
        </p>
        <div className="landing-cta">
          <button className="primary-btn lg" onClick={onGetStarted}>
            Create your account
          </button>
          <button className="secondary-btn lg" onClick={onSignIn}>
            I already have one
          </button>
        </div>
        <p className="landing-meta">Free to try · No card required</p>
      </section>

      <section id="features" className="landing-features">
        <article className="feature-card">
          <div className="feature-num">01</div>
          <h3>Upload statements</h3>
          <p>
            Drop in a CSV or XLSX export from your bank. Histr parses it in
            seconds, no formatting required.
          </p>
        </article>
        <article className="feature-card">
          <div className="feature-num">02</div>
          <h3>Automatic categories</h3>
          <p>
            Transactions are sorted into clean buckets — food, transport,
            transfers — so you can see the shape of your spending at a glance.
          </p>
        </article>
        <article className="feature-card">
          <div className="feature-num">03</div>
          <h3>Quiet insights</h3>
          <p>
            A simple ledger, totals that update live, and a small chart that
            tells you the truth. No noise, no nudges.
          </p>
        </article>
      </section>

      <section id="how" className="landing-strip">
        <div>
          <p className="eyebrow">How it works</p>
          <h2>Three steps. That's it.</h2>
        </div>
        <ol className="landing-steps">
          <li>
            <strong>Sign up.</strong> Pick a username and a password.
          </li>
          <li>
            <strong>Upload a statement.</strong> CSV or XLSX from any bank.
          </li>
          <li>
            <strong>Watch the totals.</strong> Income, expense, and net — live.
          </li>
        </ol>
      </section>

      <section className="landing-final">
        <h2>Ready when you are.</h2>
        <button className="primary-btn lg" onClick={onGetStarted}>
          Create your account
        </button>
      </section>

      <footer className="landing-footer">
        <span>© {new Date().getFullYear()} Histr</span>
        <span>A small ledger, made carefully.</span>
      </footer>
    </div>
  );
}
