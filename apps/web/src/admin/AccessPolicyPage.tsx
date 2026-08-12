import { useEffect, useState } from "react";
import { accessPolicyApi, type AccessPolicy } from "../api/client.ts";
import { useAuth } from "../auth/AuthProvider.tsx";
import { isUsable } from "../auth/oidc.ts";

export function AccessPolicyPage() {
  const { user } = useAuth();
  const [policy, setPolicy] = useState<AccessPolicy | null>(null);
  const [message, setMessage] = useState("");
  useEffect(() => {
    if (isUsable(user)) void accessPolicyApi.get(user).then(setPolicy).catch(() => setMessage("Unable to load access policy."));
  }, [user]);
  if (!isUsable(user)) return null;
  if (policy === null) return <main className="page-shell"><h1>Study access policy</h1><p>{message || "Loading policy…"}</p></main>;
  const save = async () => {
    setMessage("Saving…");
    try { setPolicy(await accessPolicyApi.update(user, policy)); setMessage("Policy saved and audited."); }
    catch { setMessage("Policy update failed."); }
  };
  return <main className="page-shell"><h1>Study access policy</h1>
    <p>Changes take effect immediately and are recorded in the security audit log.</p>
    <label><input type="checkbox" checked={policy.sharedStudyAccess} onChange={(e) => setPolicy({ ...policy, sharedStudyAccess: e.target.checked })}/> Shared candidate/interviewer access</label>
    <label><input type="checkbox" checked={policy.candidateEnabled} onChange={(e) => setPolicy({ ...policy, candidateEnabled: e.target.checked })}/> Candidate access</label>
    <label><input type="checkbox" checked={policy.interviewerEnabled} onChange={(e) => setPolicy({ ...policy, interviewerEnabled: e.target.checked })}/> Interviewer access</label>
    <button onClick={() => void save()}>Save access policy</button><p role="status">{message}</p>
  </main>;
}
