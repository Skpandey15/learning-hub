# Production Logging and Exception Handling

## Guarantees

- Every HTTP request receives or preserves a safe `X-Correlation-ID`.
- Application and edge logs are one-line JSON suitable for centralized ingestion.
- Request logs contain method, path without query string, status, and duration.
- Request bodies, query strings, authorization headers, cookies, tokens, and secrets are never intentionally logged.
- Public errors use `application/problem+json` with stable codes and safe details.
- Unexpected exception messages and implementation details never reach clients.
- Authentication and authorization failures use the same problem contract as business APIs.
- Exception logs use type/fingerprint or redacted frame information instead of arbitrary exception messages.
- Stack output is bounded to prevent log-volume denial of service.

## Spring Boot API

Spring Boot emits Elastic Common Schema JSON using native structured logging. MDC adds the correlation ID. Request completion uses SLF4J structured key/value fields. `ApiExceptionHandler` maps validation, malformed input, missing resources, unsupported methods, access denial, application errors, and unexpected failures.

Expected application failures use `ApiException` plus a stable `ErrorCode`. Only explicitly safe properties may enter the response. Unexpected failures are logged with exception type and a deterministic truncated SHA-256 fingerprint; arbitrary exception messages are excluded.

Spring Security uses dedicated `AuthenticationEntryPoint` and `AccessDeniedHandler` implementations so failures occurring before MVC still return the standard JSON contract.

## FastAPI AI service

The AI service configures JSON logging before serving requests. Its outer request middleware retains correlation context on success and failure, returns a safe problem response for otherwise unhandled exceptions, and emits one completion event.

`AppError` represents expected failures such as provider timeout, provider unavailability, invalid model output, rate limiting, and authentication failure. Validation and HTTP exceptions have explicit mappings. Exception stack output contains frames and type, is length-limited, and applies sensitive-key redaction.

## Web and edge

React uses a top-level error boundary with an opaque incident ID and safe recovery screen. It does not emit exception messages, props, tokens, or browser state. Nginx emits JSON access logs using `$uri`, deliberately excluding query strings, cookies, and request headers.

## Operational policy

- Default application level is `INFO`; security framework detail is `WARN`.
- Debug logging is temporary, environment-scoped, approved, and never enabled globally in production.
- Central storage controls access, encryption, retention, deletion, and alerting.
- Alert on 5xx rate, authentication anomalies, repeated error fingerprints, provider failures, and logging-pipeline failure.
- Logging/telemetry failure must not reveal secrets or turn safe client errors into raw exceptions.

## Verification

- Java tests prove unexpected details do not leak and security failures use problem JSON.
- Python tests prove unexpected details do not leak and correlation headers survive failures.
- React tests prove render exception details do not appear in the recovery UI or console event.
- Container builds validate the Nginx configuration and application packaging.

