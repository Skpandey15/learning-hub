import { beforeEach, describe, expect, it, vi } from "vitest";
import { accessPolicyApi } from "./client.ts";

const user = { access_token: "secret" } as never;
const policy = { sharedStudyAccess: true, candidateEnabled: true, interviewerEnabled: true, updatedAt: "now", updatedBy: "admin" };

describe("API client", () => {
  beforeEach(() => vi.stubGlobal("fetch", vi.fn()));
  it("sends bearer credentials without browser cookies", async () => {
    vi.mocked(fetch).mockResolvedValue({ ok: true, json: () => Promise.resolve(policy) } as Response);
    expect(await accessPolicyApi.get(user)).toEqual(policy);
    expect(fetch).toHaveBeenCalledWith("/api/v1/admin/access-policy", expect.objectContaining({ credentials: "omit" }));
    await accessPolicyApi.update(user, policy);
    expect(fetch).toHaveBeenLastCalledWith("/api/v1/admin/access-policy", expect.objectContaining({ method: "PUT" }));
  });
  it("rejects non-success responses", async () => {
    vi.mocked(fetch).mockResolvedValue({ ok: false, status: 403 } as Response);
    await expect(accessPolicyApi.get(user)).rejects.toThrow("status 403");
  });
});
