import type { ReactNode } from "react";
import { useAuth } from "./AuthProvider.tsx";
import { isUsable } from "./oidc.ts";

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const auth = useAuth();
  if (auth.loading) return <main className="page-shell"><p>Checking your secure session…</p></main>;
  if (!isUsable(auth.user)) {
    return <main className="page-shell"><h1>Sign in required</h1><p>Authentication is required to access study material.</p><button onClick={() => void auth.login()}>Sign in securely</button></main>;
  }
  return children;
}
