import { Navigate, Route, Routes } from "react-router-dom";

function LearningHome() {
  return (
    <main id="main-content" className="page-shell">
      <p className="eyebrow">Production foundation</p>
      <h1>Learning Hub</h1>
      <p className="lede">
        Structured engineering study material, generated responsibly and saved for consistent
        learning.
      </p>
      <section aria-labelledby="status-heading" className="status-card">
        <h2 id="status-heading">Platform setup in progress</h2>
        <p>The curriculum catalog and authenticated study experience are coming next.</p>
      </section>
    </main>
  );
}

export function App() {
  return (
    <Routes>
      <Route path="/learn" element={<LearningHome />} />
      <Route path="*" element={<Navigate to="/learn" replace />} />
    </Routes>
  );
}
