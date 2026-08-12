import { Navigate, Route, Routes } from "react-router-dom";
import { AuthCallback } from "./auth/AuthCallback.tsx";
import { ProtectedRoute } from "./auth/ProtectedRoute.tsx";
import { useAuth } from "./auth/AuthProvider.tsx";
import { AccessPolicyPage } from "./admin/AccessPolicyPage.tsx";

function LearningHome() {
  const auth = useAuth();
  return (
    <main id="main-content" className="page-shell">
      <div className="topbar"><p className="eyebrow">Secure learning workspace</p><button onClick={() => void auth.logout()}>Sign out</button></div>
      <h1>Learning Hub</h1>
      <p className="lede">
        Structured engineering study material, generated responsibly and saved for consistent
        learning.
      </p>
      <section aria-labelledby="status-heading" className="status-card">
        <h2 id="status-heading">Authenticated access enabled</h2>
        <p>Signed in as {auth.user?.profile.preferred_username ?? auth.user?.profile.sub}.</p>
      </section>
    </main>
  );
}

export function App() {
  return (
    <Routes>
      <Route path="/learn" element={<ProtectedRoute><LearningHome /></ProtectedRoute>} />
      <Route path="/auth/callback" element={<AuthCallback />} />
      <Route path="/admin/access-policy" element={<ProtectedRoute><AccessPolicyPage /></ProtectedRoute>} />
      <Route path="/signed-out" element={<main className="page-shell"><h1>Signed out</h1><p>Your local session has ended.</p></main>} />
      <Route path="*" element={<Navigate to="/learn" replace />} />
    </Routes>
  );
}
