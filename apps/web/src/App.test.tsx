import { fireEvent, render, screen } from "@testing-library/react";
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
  it("renders the learning hub foundation", () => {
    render(
      <MemoryRouter initialEntries={["/learn"]}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: "Learning Hub", level: 1 })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));
  });

  it("falls back to the subject when username is absent", () => {
    authState.username = undefined;
    render(<MemoryRouter initialEntries={["/learn"]}><App /></MemoryRouter>);
    expect(screen.getByText("Signed in as 1.")).toBeInTheDocument();
    authState.username = "alice";
  });
});
