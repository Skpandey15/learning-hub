import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { userManager } from "./oidc.ts";

export function AuthCallback() {
  const navigate = useNavigate();
  const [failed, setFailed] = useState(false);
  useEffect(() => {
    void userManager.signinRedirectCallback().then(() => navigate("/learn", { replace: true })).catch(() => setFailed(true));
  }, [navigate]);
  return <main className="page-shell"><h1>{failed ? "Sign-in failed" : "Completing sign-in…"}</h1>{failed && <p>Please return to sign in and try again.</p>}</main>;
}
