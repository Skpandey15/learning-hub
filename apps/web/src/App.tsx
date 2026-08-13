import { Navigate, Route, Routes } from "react-router-dom";
import { AuthCallback } from "./auth/AuthCallback.tsx";
import { ProtectedRoute } from "./auth/ProtectedRoute.tsx";
import { AccessPolicyPage } from "./admin/AccessPolicyPage.tsx";
import { CatalogAdminPage } from "./admin/CatalogAdminPage.tsx";
import { LearningCatalogPage } from "./learning/LearningCatalogPage.tsx";
import { LessonPage } from "./learning/LessonPage.tsx";

export function App() {
  return (
    <Routes>
      <Route path="/learn" element={<ProtectedRoute><LearningCatalogPage /></ProtectedRoute>} />
      <Route path="/learn/topics/:topicId" element={<ProtectedRoute><LessonPage /></ProtectedRoute>} />
      <Route path="/oidc/callback" element={<AuthCallback />} />
      <Route path="/admin/access-policy" element={<ProtectedRoute><AccessPolicyPage /></ProtectedRoute>} />
      <Route path="/admin/catalog" element={<ProtectedRoute><CatalogAdminPage /></ProtectedRoute>} />
      <Route path="/signed-out" element={<main className="page-shell"><h1>Signed out</h1><p>Your local session has ended.</p></main>} />
      <Route path="*" element={<Navigate to="/learn" replace />} />
    </Routes>
  );
}
