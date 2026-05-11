import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "./auth";

const authPageClass = "grid min-h-screen place-items-center px-4 py-8";
const authCardClass =
  "grid w-full max-w-[380px] gap-4 rounded-xl border border-gray-200 bg-white p-8";

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

    completeOAuthLogin({
      accessToken,
      refreshToken,
      username: searchParams.get("username") ?? searchParams.get("name"),
      profilePicture:
        searchParams.get("profilePicture") ??
        searchParams.get("picture") ??
        searchParams.get("avatar"),
    });
    navigate("/dashboard", { replace: true });
  }, [completeOAuthLogin, navigate, searchParams]);

  return (
    <div className={authPageClass} aria-busy={!error}>
      <div className={authCardClass}>
        <div className="-mt-1 mb-1 flex items-center gap-2.5">
          <span className="grid h-9 w-9 place-items-center rounded-lg bg-gray-900 font-bold text-white">
            H
          </span>
          <span className="text-[1.05rem] font-bold">Histr</span>
        </div>

        {error ? (
          <>
            <h1 className="text-[1.4rem] font-bold">Sign in failed</h1>
            <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </p>
          </>
        ) : (
          <>
            <h1 className="text-[1.4rem] font-bold">Signing you in</h1>
            <p className="text-sm text-gray-500">
              Finishing your Google sign in.
            </p>
          </>
        )}
      </div>
    </div>
  );
}
