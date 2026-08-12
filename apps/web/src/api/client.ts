import type { User } from "oidc-client-ts";

export type AccessPolicy = {
  sharedStudyAccess: boolean;
  candidateEnabled: boolean;
  interviewerEnabled: boolean;
  updatedAt: string;
  updatedBy: string;
};

async function request<T>(path: string, user: User, init?: RequestInit): Promise<T> {
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
