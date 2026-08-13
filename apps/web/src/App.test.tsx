import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { App } from "./App.tsx";

const authState = vi.hoisted<{ username?: string }>(() => ({ username: "alice" }));

vi.mock("./auth/AuthProvider.tsx", () => ({
  useAuth: () => ({
    loading: false,
    user: { expired: false, access_token: "token", profile: { sub: "1", preferred_username: authState.username } },
    login: vi.fn(),
    logout: vi.fn(),
  }),
}));

describe("App", () => {
  it("renders the learning catalog and signs out", async () => {
    vi.stubGlobal("fetch", vi.fn()
      .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve([]) })
      .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ completedUnits: 0, totalUnits: 0, percent: 0 }) }));
    render(
      <MemoryRouter initialEntries={["/learn"]}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: "Learning Hub", level: 1 })).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText("No published ecosystems yet.")).toBeInTheDocument());
    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));
  });

  it("renders the signed-out route", () => {
    render(<MemoryRouter initialEntries={["/signed-out"]}><App /></MemoryRouter>);
    expect(screen.getByRole("heading", { name: "Signed out" })).toBeInTheDocument();
  });
});
