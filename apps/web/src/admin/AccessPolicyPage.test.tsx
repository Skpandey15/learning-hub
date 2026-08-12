import { fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({ get: vi.fn(), update: vi.fn(), usable: true }));
vi.mock("../auth/AuthProvider.tsx", () => ({ useAuth: () => ({ user: { access_token: "token" } }) }));
vi.mock("../auth/oidc.ts", () => ({ isUsable: () => mocks.usable }));
vi.mock("../api/client.ts", () => ({ accessPolicyApi: { get: mocks.get, update: mocks.update } }));
import { AccessPolicyPage } from "./AccessPolicyPage.tsx";

const policy = { sharedStudyAccess: true, candidateEnabled: true, interviewerEnabled: true, updatedAt: "now", updatedBy: "admin" };
describe("AccessPolicyPage", () => {
  beforeEach(() => { vi.clearAllMocks(); mocks.usable = true; mocks.get.mockResolvedValue(policy); mocks.update.mockResolvedValue(policy); });
  it("loads, edits, and saves policy", async () => {
    render(<AccessPolicyPage />);
    const candidate = await screen.findByRole("checkbox", { name: "Candidate access" });
    fireEvent.click(screen.getByRole("checkbox", { name: "Shared candidate/interviewer access" }));
    fireEvent.click(candidate);
    fireEvent.click(screen.getByRole("checkbox", { name: "Interviewer access" }));
    fireEvent.click(screen.getByRole("button", { name: "Save access policy" }));
    await screen.findByText("Policy saved and audited.");
    expect(mocks.update).toHaveBeenCalledWith(expect.anything(), expect.objectContaining({ sharedStudyAccess: false, candidateEnabled: false, interviewerEnabled: false }));
  });
  it("reports load and update failures", async () => {
    mocks.get.mockRejectedValueOnce(new Error("failed"));
    const first = render(<AccessPolicyPage />);
    await screen.findByText("Unable to load access policy."); first.unmount();
    mocks.get.mockResolvedValueOnce(policy); mocks.update.mockRejectedValueOnce(new Error("failed"));
    const second = render(<AccessPolicyPage />); const scoped = within(second.container);
    await scoped.findByRole("checkbox", { name: "Candidate access" });
    fireEvent.click(scoped.getByRole("button", { name: "Save access policy" }));
    await screen.findByText("Policy update failed.");
  });
  it("renders nothing without a usable session", () => {
    mocks.usable = false;
    const { container } = render(<AccessPolicyPage />);
    expect(container).toBeEmptyDOMElement();
  });
});
