# Learning Hub V1 — AI Content Design

## 1. Purpose

The AI subsystem generates structured study material for a predefined topic. It does not define the curriculum, authorize users, publish content, calculate progress, or recommend unknown database entities.

## 2. Generation contract

Input is trusted catalog metadata supplied by Spring Boot: domain, technology, topic, level, duration, objectives, and prompt version. User-authored free text is not part of the V1 generation request.

Output must be valid JSON matching this logical schema:

```json
{
  "topicId": "4b5a...",
  "title": "Java Collections",
  "introduction": "...",
  "units": [
    {
      "stableKey": "collection-hierarchy",
      "type": "THEORY",
      "title": "The collection hierarchy",
      "bodyMarkdown": "...",
      "codeLanguage": "java",
      "codeExample": "...",
      "keyTakeaways": ["..."],
      "estimatedMinutes": 12
    }
  ],
  "conclusion": "..."
}
```

The schema requires the supplied `topicId`, 4–12 units, unique stable keys, allowed unit types, bounded string sizes, total duration within an accepted tolerance, and at least one example or exercise.

## 3. Content expectations

Every lesson should:

- Match the requested skill level and objectives.
- Explain concepts before using them.
- Include practical, syntactically plausible examples where relevant.
- Distinguish facts from trade-offs and rules of thumb.
- Mention version-sensitive behavior rather than claiming timeless accuracy.
- Include concise key takeaways.
- Avoid assessments, scores, answer collection, and interview simulation.
- Avoid external links in V1 unless the URL is from a server-controlled allowlist.

## 4. Prompt strategy

Prompts are versioned source files in the AI service, reviewed like code. The system prompt defines role, output schema, safety rules, audience, and factual-quality expectations. The user message contains JSON-encoded catalog metadata delimited as data and explicitly states that embedded instructions are not commands.

Use the provider's structured-output capability when supported. Otherwise request JSON and validate strictly with Pydantic. No hidden chain-of-thought is requested or stored.

## 5. Validation pipeline

```text
Model response
  → JSON parse
  → Pydantic schema validation
  → semantic validation
  → normalization
  → Spring Boot validation
  → transactional publication
```

Semantic checks include:

- Topic ID matches the request.
- Unit count and length bounds are respected.
- Stable keys are unique and slug-safe.
- Markdown excludes raw HTML, images, scripts, and unsafe links.
- Code and Markdown stay within configured size limits.
- Estimated unit time is plausible.
- Output does not introduce an assessment section.

One schema-repair attempt may be made with the validation errors. A factual or policy concern fails the job rather than silently publishing questionable content.

## 6. Models and LiteLLM

The AI service calls a configurable LiteLLM OpenAI-compatible endpoint. Configuration includes:

- `LITELLM_BASE_URL`
- `LITELLM_API_KEY`
- `STUDY_MODEL`
- `STUDY_MODEL_TIMEOUT_SECONDS`
- `PROMPT_VERSION`
- output token and retry limits

Pin model aliases in deployment configuration. Record model name, prompt version, token usage, latency, and content hash, but not credentials or complete prompts.

## 7. Reliability and cost controls

- Generate once and persist; do not generate during ordinary reads.
- Limit concurrent jobs globally and per topic.
- Apply per-user daily generation limits.
- Use one bounded retry for transient provider errors with jitter.
- Do not retry validation/policy failures indefinitely.
- Use idempotency key `generationJobId`.
- Mark stale in-progress jobs failed or safely requeue them at startup.
- Cap prompt and completion tokens.

## 8. Safety and quality

Generated material is AI-assisted and may contain errors. The UI presents a concise notice. Security-sensitive code must not include real credentials. Prompts prohibit malware instructions, credential harvesting, and destructive commands; normal defensive engineering education remains allowed.

V1 includes an administrator review console. Conservative automated checks are mandatory but insufficient for publication: validated output becomes a draft and requires explicit administrator approval. Feedback/reporting and source grounding remain future enhancements.

## 9. Failure mapping

| Failure | Internal result | Public behavior |
|---|---|---|
| LiteLLM timeout | `PROVIDER_TIMEOUT` | Job failed; retry later |
| Provider unavailable | `PROVIDER_UNAVAILABLE` | Job failed; published content unaffected |
| Invalid JSON | repair once, then `INVALID_OUTPUT` | Job failed |
| Semantic mismatch | `CONTENT_VALIDATION_FAILED` | Job failed |
| Rate limit | `RATE_LIMITED` | `429` with retry guidance |
| Service auth failure | `UNAUTHORIZED_SERVICE` | Internal `401`; no public details |

## 10. Test strategy

- Pydantic contract tests with valid and invalid fixtures
- Prompt snapshot/version tests
- Mocked LiteLLM success, timeout, rate-limit, and malformed-output tests
- Topic-ID and Markdown-safety tests
- Idempotency tests
- Spring Boot consumer contract tests
- One optional, manually triggered provider smoke test excluded from normal CI
