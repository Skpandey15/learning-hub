import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { studyApi, type Domain, type Progress, type Technology, type Topic } from "../api/client.ts";
import { useAuth } from "../auth/AuthProvider.tsx";
import { isUsable } from "../auth/oidc.ts";

export function LearningCatalogPage() {
  const { user, logout } = useAuth();
  const [domains, setDomains] = useState<Domain[]>([]);
  const [technologies, setTechnologies] = useState<Technology[]>([]);
  const [topics, setTopics] = useState<Topic[]>([]);
  const [progress, setProgress] = useState<Progress | null>(null);
  const [message, setMessage] = useState("Loading learning catalog…");
  useEffect(() => {
    if (!isUsable(user)) return;
    void Promise.all([studyApi.domains(user), studyApi.progress(user)])
      .then(([items, current]) => { setDomains(items); setProgress(current); setMessage(items.length ? "" : "No published ecosystems yet."); })
      .catch(() => setMessage("The learning catalog is temporarily unavailable."));
  }, [user]);
  if (!isUsable(user)) return null;
  const chooseDomain = async (id: string) => { setTechnologies(await studyApi.technologies(user, id)); setTopics([]); };
  const chooseTechnology = async (id: string) => setTopics(await studyApi.topics(user, id));
  return <main className="page-shell">
    <div className="topbar"><p className="eyebrow">Secure learning workspace</p><nav><Link to="/admin/catalog">Admin</Link> <button onClick={() => void logout()}>Sign out</button></nav></div>
    <h1>Learning Hub</h1><p className="lede">Choose an ecosystem, technology, and published topic.</p>
    {progress && <p role="status">Progress: {progress.completedUnits}/{progress.totalUnits} units ({progress.percent}%)</p>}
    {message && <p role="status">{message}</p>}
    <section className="catalog-grid" aria-label="Ecosystems">{domains.map((item) => <button className="catalog-card" key={item.id} onClick={() => void chooseDomain(item.id)}><strong>{item.name}</strong><span>{item.description}</span></button>)}</section>
    <section className="catalog-grid" aria-label="Technologies">{technologies.map((item) => <button className="catalog-card" key={item.id} onClick={() => void chooseTechnology(item.id)}><strong>{item.name}</strong><span>{item.description}</span></button>)}</section>
    <section className="catalog-grid" aria-label="Topics">{topics.map((item) => <Link className="catalog-card" key={item.id} to={`/learn/topics/${item.id}`}><strong>{item.title}</strong><span>{item.skillLevel} · {item.estimatedMinutes} min</span><span>{item.summary}</span></Link>)}</section>
  </main>;
}
