# Learning Hub V1 — UI/UX Design

## 1. Experience principles

- Make it obvious where the learner is and what comes next.
- Keep reading central; AI-generation mechanics remain secondary.
- Never imply that generated material is infallible.
- Preserve progress automatically and confirm completion immediately.
- Support keyboard, mobile, reduced motion, and screen readers from the start.

## 2. Information architecture

```text
Home
├── Learn
│   ├── Domain
│   │   └── Technology
│   │       └── Topic
│   │           └── Study unit
│   └── Learning path
└── My Progress
Admin
├── Ecosystems and curriculum
├── Content versions
├── Access policy
└── Audit log
```

## 3. Routes

| Route | Screen |
|---|---|
| `/` | Authenticated dashboard |
| `/learn` | Domain catalog and continue-learning section |
| `/learn/domains/:domainSlug` | Technologies in a domain |
| `/learn/domains/:domainSlug/:technologySlug` | Filterable topic catalog |
| `/learn/topics/:topicId` | Topic overview and study content |
| `/learn/paths/:pathSlug` | Ordered learning path |
| `/progress` | Personal progress dashboard |
| `/auth/callback` | OIDC callback |
| `/admin` | Admin dashboard |
| `/admin/ecosystems` | Ecosystem management |
| `/admin/ecosystems/:id` | Technology/topic hierarchy editor |
| `/admin/access` | Role-capability policy editor |
| `/admin/audit` | Read-only audit explorer |

## 4. Global shell

Desktop navigation contains logo, Learn, My Progress, user menu, and sign out. Mobile uses a compact header and accessible drawer. Breadcrumbs appear below the header on nested catalog and topic pages.

The visual language should use readable typography, restrained color, clear focus rings, and consistent cards. Skill levels must use text labels as well as color.

## 5. Screen specifications

### Dashboard

- Welcome heading
- Continue-learning card based on most recent access
- Overall progress summary
- Recommended next catalog topic determined by path/order, without an LLM
- Link to all domains

### Learning Hub

- Eight domain cards from the API
- Domain description, technology count, and progress
- Loading skeletons, retry state, and empty-state explanation

### Domain and technology pages

- Breadcrumb and summary
- Technology or topic cards in database order
- Skill-level segmented filter: All, Beginner, Intermediate, Advanced
- URL query parameter preserves the filter
- Each topic card displays estimated time, content availability, and progress

### Topic page

Published state:

- Topic title, level, estimated duration, objectives, version marker
- Table of contents on wide screens
- Sequential study units with Markdown, sanitized code blocks, takeaways, and completion control
- Previous/next unit controls
- Sticky progress indicator that does not obscure content
- Small notice: “AI-assisted material. Verify critical details against official documentation.”

Missing state:

- Topic objectives and outline remain visible
- Primary action: “Generate study material”
- Explanation that generation can take time and content will be saved

Generating state:

- Non-blocking progress panel with status text
- Poll with bounded exponential backoff
- User may navigate away and return

Failed state:

- Plain explanation and retry action when permitted
- Correlation ID available in expandable technical details
- Never show raw provider errors

Updated-version state:

- Explain that a newer lesson version is available
- Current-version progress begins separately; historical progress remains visible on Progress

### My Progress

- Overall unit-weighted completion
- Progress grouped by domain and technology
- Recently studied topics
- Completed topics
- Historical versions hidden behind an expandable “Previous versions” section

## 6. Completion interaction

Completion uses an explicit checkbox/button labeled “Mark unit complete.” Apply optimistic UI only after the request begins, roll back on error, and announce the result through an `aria-live` region. “Undo completion” remains available.

## 7. Responsive design

- Minimum supported width: 320 px.
- Cards collapse to one column on narrow screens.
- Topic table of contents becomes a disclosure menu.
- Code blocks scroll horizontally without forcing page width.
- Touch targets are at least 44 × 44 CSS pixels.

## 8. Accessibility

Target WCAG 2.2 AA:

- Semantic landmarks and one primary page heading
- Keyboard-operable navigation and controls
- Visible focus state
- Contrast-compliant text and controls
- Form labels and descriptive errors
- Status announcements for generation and completion
- No color-only meaning
- Reduced-motion support
- Sanitized Markdown with correct heading hierarchy

## 9. Client architecture

```text
src/
├── api/
├── auth/
├── components/
│   ├── catalog/
│   ├── content/
│   └── progress/
├── pages/
├── routes/
├── types/
└── test/
```

Use TanStack Query for server state and invalidation. React component state handles view-only choices. Do not persist taxonomy, content, or progress in local storage. OIDC library-managed session storage is allowed when configured securely.

## 10. Analytics and privacy

V1 may record aggregate page and generation events only after a privacy decision. Do not send study-content bodies, access tokens, or identity-provider claims to analytics. Product analytics is not required for launch.

## 11. Admin console

The admin shell is visually distinct and is never rendered from navigation without an `ADMIN_*` capability. Server authorization remains authoritative.

- Ecosystem management supports create, draft, preview, publish, reorder, deactivate, and archive.
- A hierarchy editor manages technologies, topics, objectives, levels, prerequisites, and paths.
- Unsaved-change protection and optimistic-conflict resolution prevent silent overwrites.
- Publication shows validation errors and requires explicit confirmation.
- Access Policy presents a role × capability matrix, highlights deny precedence, shows the effective diff, requires a reason, and requests step-up authentication before save.
- Audit Log is filterable and read-only.
- Destructive-looking actions use archive, explain learner impact, and never rely on color alone.
