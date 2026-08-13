import type { User } from "oidc-client-ts";

export type AccessPolicy = {
  sharedStudyAccess: boolean;
  candidateEnabled: boolean;
  interviewerEnabled: boolean;
  updatedAt: string;
  updatedBy: string;
};

export type Domain = { id: string; slug: string; name: string; description: string; displayOrder: number; status: string; version: number };
export type Technology = { id: string; domainId: string; slug: string; name: string; description: string; displayOrder: number; status: string; version: number };
export type Topic = { id: string; technologyId: string; slug: string; title: string; summary: string; skillLevel: string; estimatedMinutes: number; objectives: string[]; displayOrder: number; status: string; currentContentVersionId?: string; version: number };
export type StudyUnit = { id: string; stableKey: string; type: string; title: string; bodyMarkdown: string; codeLanguage?: string; codeExample?: string; keyTakeaways: string[]; displayOrder: number; estimatedMinutes: number; completed: boolean };
export type Content = { versionId: string; topicId: string; versionNumber: number; title: string; introduction: string; conclusion: string; modelName: string; promptVersion: string; units: StudyUnit[] };
export type GenerationJob = { id: string; topicId: string; status: string; requestedAt: string; completedAt?: string; resultVersionId?: string; errorCode?: string };
export type Progress = { completedUnits: number; totalUnits: number; percent: number };

export async function request<T>(path: string, user: User, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { "Authorization": `Bearer ${user.access_token}`, "Content-Type": "application/json", ...init?.headers },
    credentials: "omit",
  });
  if (!response.ok) throw new Error(`Request failed with status ${response.status}`);
  return response.json() as Promise<T>;
}

export const accessPolicyApi = {
  get: (user: User) => request<AccessPolicy>("/api/v1/admin/access-policy", user),
  update: (user: User, policy: Pick<AccessPolicy, "sharedStudyAccess" | "candidateEnabled" | "interviewerEnabled">) =>
    request<AccessPolicy>("/api/v1/admin/access-policy", user, { method: "PUT", body: JSON.stringify(policy) }),
};

export const studyApi = {
  domains: (user: User) => request<Domain[]>("/api/v1/learning/domains", user),
  technologies: (user: User, domainId: string) => request<Technology[]>(`/api/v1/learning/domains/${domainId}/technologies`, user),
  topics: (user: User, technologyId: string) => request<Topic[]>(`/api/v1/learning/technologies/${technologyId}/topics`, user),
  content: (user: User, topicId: string) => request<Content>(`/api/v1/learning/topics/${topicId}/content`, user),
  progress: (user: User) => request<Progress>("/api/v1/learning/progress/me", user),
  complete: (user: User, unitId: string, completed: boolean) => request(`/api/v1/learning/units/${unitId}/completion`, user, { method: "PUT", body: JSON.stringify({ completed }) }),
};

export const catalogAdminApi = {
  domains: (user: User) => request<Domain[]>("/api/v1/admin/ecosystems", user),
  createDomain: (user: User, body: object) => request<Domain>("/api/v1/admin/ecosystems", user, { method: "POST", body: JSON.stringify(body) }),
  createTechnology: (user: User, domainId: string, body: object) => request<Technology>(`/api/v1/admin/ecosystems/${domainId}/technologies`, user, { method: "POST", body: JSON.stringify(body) }),
  createTopic: (user: User, technologyId: string, body: object) => request<Topic>(`/api/v1/admin/technologies/${technologyId}/topics`, user, { method: "POST", body: JSON.stringify(body) }),
  publishDomain: (user: User, domain: Domain, reason: string) => request<Domain>(`/api/v1/admin/ecosystems/${domain.id}/publish`, user, { method: "POST", headers: { "If-Match": String(domain.version) }, body: JSON.stringify({ reason }) }),
  generate: (user: User, topicId: string) => request<GenerationJob>(`/api/v1/admin/topics/${topicId}/generation-jobs`, user, { method: "POST", headers: { "Idempotency-Key": crypto.randomUUID() } }),
  job: (user: User, jobId: string) => request<GenerationJob>(`/api/v1/admin/generation-jobs/${jobId}`, user),
  draft: (user: User, versionId: string) => request<Content>(`/api/v1/admin/content-versions/${versionId}`, user),
  publishContent: (user: User, versionId: string, reason: string) => request<Content>(`/api/v1/admin/content-versions/${versionId}/publish`, user, { method: "POST", headers: { "If-Match": "0" }, body: JSON.stringify({ reason }) }),
};
