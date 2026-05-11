type Props = {
  onSignIn: () => void;
  onGetStarted: () => void;
};

const buttonBase =
  "rounded-lg border px-3.5 py-2 text-sm font-medium transition-colors disabled:cursor-default disabled:opacity-50";
const primaryButton = `${buttonBase} border-gray-900 bg-gray-900 text-white enabled:hover:border-gray-800 enabled:hover:bg-gray-800`;
const secondaryButton = `${buttonBase} border-gray-200 bg-white text-gray-900 enabled:hover:border-gray-300`;
const largeButton = "px-5 py-3";

const features = [
  {
    number: "01",
    title: "Upload statements",
    description:
      "Drop in a CSV or XLSX export from your bank. Histr parses it in seconds, no formatting required.",
  },
  {
    number: "02",
    title: "Automatic categories",
    description:
      "Transactions are sorted into clean buckets — food, transport, transfers — so you can see the shape of your spending at a glance.",
  },
  {
    number: "03",
    title: "Quiet insights",
    description:
      "A simple ledger, totals that update live, and a small chart that tells you the truth. No noise, no nudges.",
  },
];

const steps = [
  ["01", "Sign up.", "Pick a username and a password."],
  ["02", "Upload a statement.", "CSV or XLSX from any bank."],
  ["03", "Watch the totals.", "Income, expense, and net — live."],
];

export function Landing({ onSignIn, onGetStarted }: Props) {
  return (
    <div className="grid min-h-screen grid-rows-[auto_1fr_auto_auto_auto_auto]">
      <header className="mx-auto flex w-full max-w-[1100px] items-center justify-between gap-4 px-4 py-5">
        <div className="flex items-center gap-3">
          <span className="grid h-9 w-9 place-items-center rounded-lg bg-gray-900 font-bold text-white">
            H
          </span>
          <span className="text-[1.05rem] font-bold">Histr</span>
        </div>
        <nav className="flex items-center gap-2">
          <a
            className="rounded-lg px-3 py-2 text-sm text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-900 max-sm:hidden"
            href="#features"
          >
            Features
          </a>
          <a
            className="rounded-lg px-3 py-2 text-sm text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-900 max-sm:hidden"
            href="#how"
          >
            How it works
          </a>
          <button className={secondaryButton} onClick={onSignIn}>
            Sign in
          </button>
          <button className={primaryButton} onClick={onGetStarted}>
            Get started
          </button>
        </nav>
      </header>

      <section className="mx-auto my-12 grid w-full max-w-[820px] justify-items-center gap-5 px-4 text-center">
        <p className="rounded-full border border-gray-200 bg-white px-3 py-1 text-xs uppercase tracking-[0.12em] text-gray-500">
          Personal financial dashboard
        </p>
        <h1 className="max-w-[18ch] text-4xl font-bold leading-[1.05] tracking-normal sm:text-6xl">
          Make sense of every{" "}
          <em className="font-medium text-gray-500">naira</em> you spend.
        </h1>
        <p className="max-w-[56ch] text-[1.05rem] leading-7 text-gray-500">
          Upload your bank statements, let Histr categorize the noise, and see
          where your money actually goes — all in one quiet dashboard.
        </p>
        <div className="mt-1 flex flex-wrap justify-center gap-2">
          <button
            className={`${primaryButton} ${largeButton}`}
            onClick={onGetStarted}
          >
            Create your account
          </button>
          <button
            className={`${secondaryButton} ${largeButton}`}
            onClick={onSignIn}
          >
            I already have one
          </button>
        </div>
        <p className="text-sm text-gray-500">Free to try · No card required</p>
      </section>

      <section
        id="features"
        className="mx-auto my-12 grid w-full max-w-[1100px] gap-4 px-4 lg:grid-cols-3"
      >
        {features.map((feature) => (
          <article
            key={feature.number}
            className="grid content-start gap-2 rounded-xl border border-gray-200 bg-white p-6"
          >
            <div className="text-xs font-semibold tracking-[0.06em] text-gray-500">
              {feature.number}
            </div>
            <h3 className="text-[1.05rem] font-semibold">{feature.title}</h3>
            <p className="text-sm leading-6 text-gray-500">
              {feature.description}
            </p>
          </article>
        ))}
      </section>

      <section
        id="how"
        className="mx-auto my-12 grid w-full max-w-[1100px] gap-8 border-y border-gray-200 px-4 py-8 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]"
      >
        <div>
          <p className="mb-1 text-xs uppercase tracking-[0.1em] text-gray-500">
            How it works
          </p>
          <h2 className="text-3xl font-bold">Three steps. That's it.</h2>
        </div>
        <ol className="grid gap-2">
          {steps.map(([number, title, description]) => (
            <li
              key={number}
              className="relative rounded-lg border border-gray-200 bg-white py-3 pr-4 pl-11 text-sm text-gray-500"
            >
              <span className="absolute top-1/2 left-4 -translate-y-1/2 text-xs tabular-nums text-gray-500">
                {number}
              </span>
              <strong className="font-semibold text-gray-900">{title}</strong>{" "}
              {description}
            </li>
          ))}
        </ol>
      </section>

      <section className="mx-auto my-12 grid w-full max-w-[820px] justify-items-center gap-4 px-4 text-center">
        <h2 className="text-3xl font-bold">Ready when you are.</h2>
        <button
          className={`${primaryButton} ${largeButton}`}
          onClick={onGetStarted}
        >
          Create your account
        </button>
      </section>

      <footer className="mx-auto flex w-full max-w-[1100px] justify-between gap-4 border-t border-gray-200 px-4 py-6 text-sm text-gray-500 max-sm:flex-col">
        <span>© {new Date().getFullYear()} Histr</span>
        <span>A small ledger, made carefully.</span>
      </footer>
    </div>
  );
}
