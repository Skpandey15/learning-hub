import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AppErrorBoundary } from "./AppErrorBoundary.tsx";

function BrokenComponent(): never {
  throw new Error("sensitive-render-detail");
}

describe("AppErrorBoundary", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("shows a safe recovery state without exposing the exception", () => {
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => undefined);
    vi.stubGlobal("location", { reload: vi.fn() });

    render(
      <AppErrorBoundary>
        <BrokenComponent />
      </AppErrorBoundary>,
    );

    expect(screen.getByRole("heading", { name: "We couldn’t display this page." })).toBeVisible();
    expect(screen.queryByText(/sensitive-render-detail/)).not.toBeInTheDocument();
    expect(consoleError).toHaveBeenCalledWith(
      "ui_render_error",
      expect.objectContaining({ incidentId: expect.any(String) as string }),
    );

    fireEvent.click(screen.getByRole("button", { name: "Reload application" }));
  });

  it("renders children when no exception occurs", () => {
    render(
      <AppErrorBoundary>
        <p>Healthy content</p>
      </AppErrorBoundary>,
    );

    expect(screen.getByText("Healthy content")).toBeVisible();
  });
});
