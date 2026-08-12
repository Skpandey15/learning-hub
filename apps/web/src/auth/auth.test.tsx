import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  getUser: vi.fn(), signinRedirect: vi.fn(), signoutRedirect: vi.fn(), callback: vi.fn(),
  addLoaded: vi.fn<(callback: (user: unknown) => void) => void>(),
  removeLoaded: vi.fn<(callback: (user: unknown) => void) => void>(),
  addUnloaded: vi.fn<(callback: () => void) => void>(), removeUnloaded: vi.fn<(callback: () => void) => void>(),
  addExpired: vi.fn<(callback: () => void) => void>(), removeExpired: vi.fn<(callback: () => void) => void>(), navigate: vi.fn(),
}));

vi.mock("./oidc.ts", () => ({
  isUsable: (user: { expired?: boolean; access_token?: string } | null) => user !== null && !user.expired && typeof user.access_token === "string",
  userManager: {
    getUser: mocks.getUser, signinRedirect: mocks.signinRedirect, signoutRedirect: mocks.signoutRedirect,
    signinRedirectCallback: mocks.callback,
    events: { addUserLoaded: mocks.addLoaded, removeUserLoaded: mocks.removeLoaded,
      addUserUnloaded: mocks.addUnloaded, removeUserUnloaded: mocks.removeUnloaded,
      addAccessTokenExpired: mocks.addExpired, removeAccessTokenExpired: mocks.removeExpired },
  },
}));
vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal<typeof import("react-router-dom")>();
  return { ...actual, useNavigate: () => mocks.navigate };
});

import { AuthProvider, useAuth } from "./AuthProvider.tsx";
import { AuthCallback } from "./AuthCallback.tsx";
import { ProtectedRoute } from "./ProtectedRoute.tsx";

function Probe() {
  const auth = useAuth();
  return <><span>{auth.loading ? "loading" : auth.user ? "signed-in" : "anonymous"}</span><button onClick={() => void auth.login()}>login</button><button onClick={() => void auth.logout()}>logout</button></>;
}

describe("authentication", () => {
  beforeEach(() => { vi.clearAllMocks(); mocks.getUser.mockResolvedValue(null); });

  it("loads a valid session and exposes login and logout", async () => {
    mocks.getUser.mockResolvedValue({ expired: false, access_token: "token" });
    const view = render(<AuthProvider><Probe /></AuthProvider>);
    expect(screen.getByText("loading")).toBeInTheDocument();
    await screen.findByText("signed-in");
    fireEvent.click(screen.getByText("login")); fireEvent.click(screen.getByText("logout"));
    expect(mocks.signinRedirect).toHaveBeenCalled(); expect(mocks.signoutRedirect).toHaveBeenCalled();
    const loaded = mocks.addLoaded.mock.calls.at(0)?.at(0);
    const unloaded = mocks.addUnloaded.mock.calls.at(0)?.at(0);
    const expired = mocks.addExpired.mock.calls.at(0)?.at(0);
    if (loaded === undefined || unloaded === undefined || expired === undefined) throw new Error("event handlers not registered");
    loaded({ expired: false, access_token: "new-token" });
    unloaded();
    expired();
    expect(mocks.signinRedirect).toHaveBeenCalledTimes(2);
    view.unmount();
    expect(mocks.removeLoaded).toHaveBeenCalledWith(loaded);
    expect(mocks.removeUnloaded).toHaveBeenCalledWith(unloaded);
    expect(mocks.removeExpired).toHaveBeenCalledWith(expired);
  });

  it("shows a sign-in action for anonymous users", async () => {
    render(<AuthProvider><ProtectedRoute><span>private</span></ProtectedRoute></AuthProvider>);
    const button = await screen.findByRole("button", { name: "Sign in securely" });
    fireEvent.click(button); expect(mocks.signinRedirect).toHaveBeenCalled();
  });

  it("completes and reports failed callbacks", async () => {
    mocks.callback.mockResolvedValueOnce({});
    const { unmount } = render(<MemoryRouter><AuthCallback /></MemoryRouter>);
    await waitFor(() => expect(mocks.navigate).toHaveBeenCalledWith("/learn", { replace: true }));
    unmount(); mocks.callback.mockRejectedValueOnce(new Error("failed"));
    render(<MemoryRouter><AuthCallback /></MemoryRouter>);
    expect(await screen.findByRole("heading", { name: "Sign-in failed" })).toBeInTheDocument();
  });

  it("rejects use outside the provider", () => {
    function Invalid() { useAuth(); return null; }
    expect(() => render(<Invalid />)).toThrow("useAuth must be used within AuthProvider");
  });
});
