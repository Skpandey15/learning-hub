import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";

import { App } from "./App.tsx";

describe("App", () => {
  it("renders the learning hub foundation", () => {
    render(
      <MemoryRouter initialEntries={["/learn"]}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: "Learning Hub", level: 1 })).toBeInTheDocument();
  });
});
