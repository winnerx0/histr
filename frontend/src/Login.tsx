import { FcGoogle } from "react-icons/fc";
import { googleOAuthUrl } from "./api";

type Props = {
  onSwitch: () => void;
  onBack: () => void;
};

const authPageClass = "relative grid min-h-screen place-items-center px-4 py-8";
const authCardClass =
  "grid w-full max-w-[380px] gap-4 p-8";
const oauthButtonClass =
  "inline-flex min-h-10 items-center justify-center gap-2 rounded-full border border-gray-200 bg-white px-3.5 py-2 text-sm font-semibold text-gray-900 no-underline transition-colors hover:border-gray-300 hover:bg-gray-50";

export function Login({ onSwitch, onBack }: Props) {
  return (
    <div className={authPageClass}>

      <div className={authCardClass}>
        <div className="flex items-center flex-col">
          <div className="-mt-1 mb-1 flex items-center gap-2.5 flex-col">
          <span className="grid h-9 w-9 place-items-center rounded-lg bg-gray-900 font-bold text-white">
            H
          </span>
          <span className="text-[1.05rem] font-bold">Histr</span>
        </div>

        <div>
          <h1 className="text-[1.4rem] font-bold">Welcome back</h1>
          <p className="mt-1 text-sm text-gray-500">
            Sign in to your Histr account.
          </p>
        </div>

        </div>
        <a className={oauthButtonClass} href={googleOAuthUrl}>
          <FcGoogle className="h-5 w-5 shrink-0" aria-hidden="true" />
          <span>Continue with Google</span>
        </a>

        {/* Email/password login is temporarily disabled. */}

      
      </div>
    </div>
  );
}
