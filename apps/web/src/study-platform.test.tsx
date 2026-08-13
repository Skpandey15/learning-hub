import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "./App.tsx";
import { CatalogAdminPage } from "./admin/CatalogAdminPage.tsx";
import { LearningCatalogPage } from "./learning/LearningCatalogPage.tsx";
import { LessonPage } from "./learning/LessonPage.tsx";

const user = { expired: false, access_token: "token", profile: { sub: "admin", preferred_username: "admin" } };
let currentUser: typeof user | null = user;
vi.mock("./auth/AuthProvider.tsx", () => ({ useAuth: () => ({ loading: false, user: currentUser, login: vi.fn(), logout: vi.fn() }) }));

const domain = { id: "d1", slug: "java", name: "Java", description: "Java ecosystem", displayOrder: 1, status: "DRAFT", version: 0 };
const publishedDomain = { ...domain, status: "PUBLISHED", version: 1 };
const technology = { id: "t1", domainId: "d1", slug: "core-java", name: "Core Java", description: "Language", displayOrder: 1, status: "DRAFT", version: 0 };
const topic = { id: "x1", technologyId: "t1", slug: "collections", title: "Collections", summary: "Use collections", skillLevel: "BEGINNER", estimatedMinutes: 60, objectives: ["Compare types"], displayOrder: 1, status: "DRAFT", version: 0 };
const content = { versionId: "v1", topicId: "x1", versionNumber: 1, title: "Collections", introduction: "Introduction", conclusion: "Conclusion", modelName: "gpt", promptVersion: "v1", units: [{ id: "u1", stableKey: "intro", type: "THEORY", title: "Lists", bodyMarkdown: "Detailed content", codeExample: "List.of();", keyTakeaways: ["Choose well"], displayOrder: 1, estimatedMinutes: 10, completed: false }] };
const response = (body: unknown) => Promise.resolve({ ok: true, json: () => Promise.resolve(body) } as Response);

describe("study platform pages", () => {
  beforeEach(() => { currentUser = user; vi.stubGlobal("fetch", vi.fn()); });
  afterEach(() => cleanup());

  it("does not render protected page content without a usable user", () => {
    currentUser = null;
    const admin = render(<CatalogAdminPage />); expect(admin.container).toBeEmptyDOMElement(); admin.unmount();
    const catalog = render(<MemoryRouter><LearningCatalogPage /></MemoryRouter>); expect(catalog.container).toBeEmptyDOMElement(); catalog.unmount();
    const lesson = render(<MemoryRouter><LessonPage /></MemoryRouter>); expect(lesson.container).toBeEmptyDOMElement();
    expect(fetch).not.toHaveBeenCalled();
  });

  it("shows empty and unavailable catalog states", async () => {
    vi.mocked(fetch).mockImplementationOnce(() => response([])).mockImplementationOnce(() => response({ completedUnits: 0, totalUnits: 0, percent: 0 }));
    const view = render(<MemoryRouter><LearningCatalogPage /></MemoryRouter>);
    expect(await screen.findByText("No published ecosystems yet.")).toBeInTheDocument();
    view.unmount();
    vi.mocked(fetch).mockRejectedValue(new Error("offline"));
    render(<MemoryRouter><LearningCatalogPage /></MemoryRouter>);
    expect(await screen.findByText("The learning catalog is temporarily unavailable.")).toBeInTheDocument();
  });

  it("shows an admin catalog loading failure", async () => {
    vi.mocked(fetch).mockRejectedValue(new Error("offline"));
    render(<CatalogAdminPage />);
    expect(await screen.findByText("Unable to load admin catalog.")).toBeInTheDocument();
  });

  it("navigates catalog hierarchy and shows topics", async () => {
    vi.mocked(fetch)
      .mockImplementationOnce(() => response([publishedDomain]))
      .mockImplementationOnce(() => response({ completedUnits: 1, totalUnits: 4, percent: 25 }))
      .mockImplementationOnce(() => response([technology]))
      .mockImplementationOnce(() => response([topic]));
    render(<MemoryRouter initialEntries={["/learn"]}><App /></MemoryRouter>);
    fireEvent.click(await screen.findByRole("button", { name: /Java/ }));
    fireEvent.click(await screen.findByRole("button", { name: /Core Java/ }));
    expect(await screen.findByRole("link", { name: /Collections/ })).toHaveAttribute("href", "/learn/topics/x1");
    expect(screen.getByText(/25%/)).toBeInTheDocument();
  });

  it("renders lesson and records completion", async () => {
    vi.mocked(fetch).mockImplementationOnce(() => response(content)).mockImplementationOnce(() => response({ completed: true }));
    render(<MemoryRouter initialEntries={["/learn/topics/x1"]}><App /></MemoryRouter>);
    expect(await screen.findByRole("heading", { name: "Collections" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("checkbox", { name: "Complete" }));
    await waitFor(() => expect(fetch).toHaveBeenLastCalledWith("/api/v1/learning/units/u1/completion", expect.objectContaining({ method: "PUT" })));
  });

  it("renders a lesson unit without an optional code example", async () => {
    vi.mocked(fetch).mockImplementationOnce(() => response({ ...content, units: [{ ...content.units[0], codeExample: null }] }));
    render(<MemoryRouter initialEntries={["/learn/topics/x1"]}><App /></MemoryRouter>);
    expect(await screen.findByRole("heading", { name: "Lists" })).toBeInTheDocument();
    expect(document.querySelector("pre")).not.toBeInTheDocument();
  });

  it("handles missing lesson safely", async () => {
    vi.mocked(fetch).mockResolvedValue({ ok: false, status: 404 } as Response);
    render(<MemoryRouter initialEntries={["/learn/topics/missing"]}><App /></MemoryRouter>);
    expect(await screen.findByText("Study material has not been published yet.")).toBeInTheDocument();
  });

  it("creates, publishes, generates, reviews, and publishes curriculum", async () => {
    vi.stubGlobal("crypto", { randomUUID: () => "key-1" });
    const job = { id: "j1", topicId: "x1", status: "QUEUED", requestedAt: "now" };
    const readyJob = { ...job, status: "AWAITING_REVIEW", resultVersionId: "v1" };
    vi.mocked(fetch)
      .mockImplementationOnce(() => response([]))
      .mockImplementationOnce(() => response(domain))
      .mockImplementationOnce(() => response(technology))
      .mockImplementationOnce(() => response(topic))
      .mockImplementationOnce(() => response(publishedDomain))
      .mockImplementationOnce(() => response(job))
      .mockImplementationOnce(() => response(job))
      .mockImplementationOnce(() => response(readyJob))
      .mockImplementationOnce(() => response(content))
      .mockImplementationOnce(() => response(content));
    render(<MemoryRouter initialEntries={["/admin/catalog"]}><App /></MemoryRouter>);
    expect(await screen.findByLabelText("Ecosystem name")).toBeInTheDocument();
    expect(screen.getByLabelText("Ecosystem slug")).toBeInTheDocument();
    expect(screen.getByLabelText("Description")).toBeInTheDocument();
    expect(screen.getByLabelText("Display order")).toBeInTheDocument();
    const inputs = await screen.findAllByRole("textbox");
    fireEvent.change(inputs[0]!, { target: { value: "Java" } }); fireEvent.change(inputs[1]!, { target: { value: "java" } }); fireEvent.change(inputs[2]!, { target: { value: "Java ecosystem" } });
    fireEvent.click(screen.getByRole("button", { name: "Create ecosystem" }));
    await screen.findByText("Draft ecosystem created and audited.");
    const techInputs = screen.getAllByRole("textbox").slice(-3); fireEvent.change(techInputs[0]!, { target: { value: "Core Java" } }); fireEvent.change(techInputs[1]!, { target: { value: "core-java" } }); fireEvent.change(techInputs[2]!, { target: { value: "Language" } });
    fireEvent.click(screen.getByRole("button", { name: "Create technology" })); await screen.findByText("Draft technology created.");
    const topicInputs = screen.getAllByRole("textbox").slice(-4); for (const [index, value] of ["Collections", "collections", "Use collections", "Compare types"].entries()) fireEvent.change(topicInputs[index]!, { target: { value } });
    fireEvent.click(screen.getByRole("button", { name: "Create topic" })); await screen.findByText("Draft topic created.");
    fireEvent.click(screen.getByRole("button", { name: "Publish curriculum" })); await screen.findByText("Ecosystem and curriculum published.");
    fireEvent.click(screen.getByRole("button", { name: "Generate study-material draft" })); await screen.findByText("Generation queued.");
    fireEvent.click(screen.getByRole("button", { name: "Refresh job" })); await screen.findByRole("heading", { name: "Generation: QUEUED" });
    fireEvent.click(screen.getByRole("button", { name: "Refresh job" })); expect(await screen.findByText(/validated units/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Publish reviewed version" })); expect(await screen.findByText("Immutable study-material version published.")).toBeInTheDocument();
  });
});
