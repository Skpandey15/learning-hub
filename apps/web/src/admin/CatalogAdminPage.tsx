import { useEffect, useState, type FormEvent } from "react";
import { catalogAdminApi, type Content, type Domain, type GenerationJob, type Technology, type Topic } from "../api/client.ts";
import { useAuth } from "../auth/AuthProvider.tsx";
import { isUsable } from "../auth/oidc.ts";

export function CatalogAdminPage() {
  const { user } = useAuth(); const [domains, setDomains] = useState<Domain[]>([]); const [domain, setDomain] = useState<Domain | null>(null);
  const [technology, setTechnology] = useState<Technology | null>(null); const [topic, setTopic] = useState<Topic | null>(null);
  const [job, setJob] = useState<GenerationJob | null>(null); const [draft, setDraft] = useState<Content | null>(null); const [message, setMessage] = useState("");
  useEffect(() => { if (isUsable(user)) void catalogAdminApi.domains(user).then(setDomains).catch(() => setMessage("Unable to load admin catalog.")); }, [user]);
  if (!isUsable(user)) return null;
  const form = (event: FormEvent<HTMLFormElement>) => new FormData(event.currentTarget);
  const createDomain = async (event: FormEvent<HTMLFormElement>) => { event.preventDefault(); const data = form(event); const value = await catalogAdminApi.createDomain(user, { slug: data.get("slug"), name: data.get("name"), description: data.get("description"), displayOrder: Number(data.get("order")) }); setDomain(value); setDomains([...domains, value]); setMessage("Draft ecosystem created and audited."); };
  const createTechnology = async (event: FormEvent<HTMLFormElement>) => { event.preventDefault(); const data = form(event); setTechnology(await catalogAdminApi.createTechnology(user, domain!.id, { slug: data.get("slug"), name: data.get("name"), description: data.get("description"), displayOrder: 1 })); setMessage("Draft technology created."); };
  const createTopic = async (event: FormEvent<HTMLFormElement>) => { event.preventDefault(); const data = form(event); setTopic(await catalogAdminApi.createTopic(user, technology!.id, { slug: data.get("slug") as string, title: data.get("title") as string, summary: data.get("summary") as string, skillLevel: "BEGINNER", estimatedMinutes: 60, objectives: [data.get("objective") as string], displayOrder: 1 })); setMessage("Draft topic created."); };
  const publish = async () => { const value = await catalogAdminApi.publishDomain(user, domain!, "Initial curriculum publication"); setDomain(value); setMessage("Ecosystem and curriculum published."); };
  const generate = async () => { const value = await catalogAdminApi.generate(user, topic!.id); setJob(value); setMessage("Generation queued."); };
  const refresh = async () => { const value = await catalogAdminApi.job(user, job!.id); setJob(value); if (value.resultVersionId) setDraft(await catalogAdminApi.draft(user, value.resultVersionId)); };
  const publishDraft = async () => { await catalogAdminApi.publishContent(user, draft!.versionId, "Administrator reviewed for accuracy and safety"); setMessage("Immutable study-material version published."); };
  return <main className="page-shell"><p className="eyebrow">Administrative control plane</p><h1>Curriculum studio</h1><p role="status">{message}</p>
    <form className="admin-form" onSubmit={(event) => void createDomain(event)}><h2>1. Ecosystem</h2><input name="name" required placeholder="Java"/><input name="slug" required placeholder="java"/><textarea name="description" required placeholder="Description"/><input name="order" type="number" min="1" defaultValue="1"/><button>Create ecosystem</button></form>
    <div className="catalog-grid">{domains.map((item) => <button className="catalog-card" key={item.id} onClick={() => setDomain(item)}>{item.name} · {item.status}</button>)}</div>
    {domain && <form className="admin-form" onSubmit={(event) => void createTechnology(event)}><h2>2. Technology in {domain.name}</h2><input name="name" required placeholder="Core Java"/><input name="slug" required placeholder="core-java"/><textarea name="description" required/><button>Create technology</button></form>}
    {technology && <form className="admin-form" onSubmit={(event) => void createTopic(event)}><h2>3. Topic in {technology.name}</h2><input name="title" required placeholder="Collections"/><input name="slug" required placeholder="collections"/><textarea name="summary" required/><input name="objective" required placeholder="Learning objective"/><button>Create topic</button></form>}
    {topic && domain?.status === "DRAFT" && <button onClick={() => void publish()}>Publish curriculum</button>}{topic && domain?.status === "PUBLISHED" && <button onClick={() => void generate()}>Generate study-material draft</button>}
    {job && <section className="status-card"><h2>Generation: {job.status}</h2><button onClick={() => void refresh()}>Refresh job</button></section>}
    {draft && <section className="lesson-unit"><h2>Review: {draft.title}</h2><p>{draft.introduction}</p><p>{draft.units.length} validated units · {draft.modelName} · {draft.promptVersion}</p><button onClick={() => void publishDraft()}>Publish reviewed version</button></section>}
  </main>;
}
