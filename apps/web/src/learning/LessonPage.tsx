import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { studyApi, type Content } from "../api/client.ts";
import { useAuth } from "../auth/AuthProvider.tsx";
import { isUsable } from "../auth/oidc.ts";

export function LessonPage() {
  const { topicId = "" } = useParams(); const { user } = useAuth();
  const [content, setContent] = useState<Content | null>(null); const [message, setMessage] = useState("Loading lesson…");
  useEffect(() => { if (isUsable(user)) void studyApi.content(user, topicId).then((value) => { setContent(value); setMessage(""); }).catch(() => setMessage("Study material has not been published yet.")); }, [topicId, user]);
  if (!isUsable(user)) return null;
  const toggle = async (unitId: string, completed: boolean) => { await studyApi.complete(user, unitId, completed); setContent((current) => current && ({ ...current, units: current.units.map((unit) => unit.id === unitId ? { ...unit, completed } : unit) })); };
  return <main className="page-shell"><Link to="/learn">← Catalog</Link>{message && <p role="status">{message}</p>}{content && <>
    <p className="eyebrow">Version {content.versionNumber} · AI-assisted, administrator reviewed</p><h1>{content.title}</h1><p className="lede">{content.introduction}</p>
    {content.units.map((unit) => <article className="lesson-unit" key={unit.id}><div className="topbar"><h2>{unit.title}</h2><label><input type="checkbox" checked={unit.completed} onChange={(event) => void toggle(unit.id, event.target.checked)}/>Complete</label></div><p className="study-copy">{unit.bodyMarkdown}</p>{unit.codeExample && <pre><code>{unit.codeExample}</code></pre>}<ul>{unit.keyTakeaways.map((takeaway) => <li key={takeaway}>{takeaway}</li>)}</ul></article>)}
    <p className="lede">{content.conclusion}</p></>}</main>;
}
