import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { User } from "oidc-client-ts";
import { isUsable, userManager } from "./oidc.ts";

type AuthState = {
  loading: boolean;
  user: User | null;
  login: () => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loaded = (next: User) => setUser(next);
    const unloaded = () => setUser(null);
    const expired = () => { setUser(null); void userManager.signinRedirect(); };
    userManager.events.addUserLoaded(loaded);
    userManager.events.addUserUnloaded(unloaded);
    userManager.events.addAccessTokenExpired(expired);
    void userManager.getUser().then((current) => setUser(isUsable(current) ? current : null)).finally(() => setLoading(false));
    return () => {
      userManager.events.removeUserLoaded(loaded);
      userManager.events.removeUserUnloaded(unloaded);
      userManager.events.removeAccessTokenExpired(expired);
    };
  }, []);

  const login = useCallback(() => userManager.signinRedirect(), []);
  const logout = useCallback(() => userManager.signoutRedirect(), []);
  const value = useMemo(() => ({ loading, user, login, logout }), [loading, user, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (value === null) throw new Error("useAuth must be used within AuthProvider");
  return value;
}
