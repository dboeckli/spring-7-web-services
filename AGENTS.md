# AGENTS.md

## Purpose

This is a technical learning project for understanding web service patterns (SOAP, Apache Camel, OpenTelemetry). The business logic (`CountryRepository` with hardcoded country data) is intentionally trivial — it exists only as a payload for the technical plumbing. Do not suggest improvements to the business logic; focus exclusively on the technical integration patterns.

## Project identity

- **Artifact**: `ch.dboeckli.spring.soap.service:spring-7-web-services:0.0.1-SNAPSHOT`
- **Java 25**, Spring Boot 4.1.0, Camel Spring Boot 4.21.0
- **SCM**: `https://github.com/dboeckli/spring-7-web-services`

## Architecture

The project exposes three SOAP interface versions and one REST interface:

| Version    | Entry Point                                            | Flow                                                                                         |
| ---------- | ------------------------------------------------------ | -------------------------------------------------------------------------------------------- |
| V1 (SOAP)  | Spring-WS `@Endpoint` (`CountrySoapEndpoint`)          | SOAP Client → Spring-WS Endpoint → `CountryRepository`                                       |
| V2 (SOAP)  | Spring-WS `@Endpoint` (`CountrySoapEndpointV2`)        | SOAP Client → Spring-WS Endpoint → Camel `direct:get-country` → `CountryRepository`          |
| V3 (SOAP)  | Apache Camel `spring-ws:` component                    | SOAP Client → Camel `spring-ws:rootqname` → Camel `direct:get-country` → `CountryRepository` |
| REST       | Spring MVC `@RestController` (`CountryRestController`) | HTTP Client → `GET /api/countries/{name}` → `CountryRepository`                              |

Key design decisions:

- V2 and V3 share `direct:get-country` to avoid duplicating business logic. V1 and REST access `CountryRepository` directly.
- V2 uses Spring-WS as the HTTP entry point and delegates to Camel via `ProducerTemplate`; V3 lets Camel own the HTTP entry point via the `spring-ws:` component.
- `direct:get-country` handles JAXB marshalling/unmarshalling and OpenTelemetry baggage propagation for both V2 and V3.

## Package layout (main)

| Package | Contents |
|---|---|
| `soap` | `CountrySoapEndpoint` (V1), `CountrySoapEndpointV2` (V2) |
| `soap/config` | `WebServiceConfig` (WSDL bean), `CustomEndpointInterceptor` |
| `camel` | `CountryCamelRoute` (V3 entry + `direct:get-country`), `SoapEndpointConfig` (beans for V3) |
| `rest` | `CountryRestController` (`GET /api/countries/{name}`) |
| `tracing` | 4 filter classes (`BaggageTagging`, `PayloadLogging`, `TraceDebug`, `TraceParent`) |
| `log` | `RequestLoggingConfig`, `ConfigChangeListener`, `LogMessage` |
| root | `CountryRepository`, `SpringSoapServiceApplication` |

## Build & test commands

```bash
./mvnw clean install                                        # full build (includes tests, Helm, Docker image)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local    # run locally with observability config
./mvnw generate-sources                                     # regenerate JAXB classes from XSD
docker compose -f compose.yaml up -d                       # start observability stack (OTel, Elasticsearch, Kibana, APM)
```

- **Maven wrapper** (`./mvnw`) — no local Maven install needed.
- **Spring Boot layers** enabled for Docker images.
- **JaCoCo** for coverage (XML report to `target/site/jacoco/`).
- **CycloneDX** for SBOM, **git-commit-id** for build metadata.
- **Docker image** build at `install` phase via `spring-boot-maven-plugin:build-image-no-fork` (uses Buildpacks, `BP_JVM_VERSION=25`).
- **Helm** processed in `install` phase: lint → template → dry-run → package.
- **POM sort** enforced by Spotless — alphabetically ordered within sections.

After changing code, always verify: run the relevant Maven goal above and report its output (evidence, not just "done").

## Sandbox build quirk (background)

This sandbox mounts the repo via filesystem passthrough, which blocks symlinks — Spotless's `npm install` (prettier) would fail with `EPERM` unless npm skips bin links. The sandbox kit sets `npm_config_bin_links=false` globally (`spec.yaml` → `environment.variables`), so no manual export is needed here. On a normal host (Windows/CI) this does not apply either.

## Formatting is enforced (fails the `validate` phase)

Two formatters are enforced at the `validate` phase and will fail the build:

```bash
./mvnw spring-javaformat:apply   # fix Java code style (Spring conventions)
./mvnw spotless:apply            # fix POM, Markdown, JSON, YAML, shell scripts
```

Always run both before committing. `spring-javaformat` applies Spring's Java style and uses **spaces** (configured in `.springjavaformatconfig`); `spotless` handles everything else including POM sort order. Spotless flexmark also formats markdown, so any `.md` edits must stay flexmark-clean; run `./mvnw spotless:apply` after editing markdown.

## Testing

Tests follow Maven naming conventions: `*Test` runs via Surefire (unit/slice), `*IT` runs via Failsafe (integration).

```bash
./mvnw test                                           # unit and slice tests only
./mvnw verify                                         # all tests including integration tests
./mvnw test -Dtest=CountrySoapEndpointSliceTest       # single unit/slice test
./mvnw failsafe:integration-test -Dit.test=CountrySoapEndpointIT  # single integration test
```

- **JUnit extension auto-detection** enabled (`junit-platform.properties`). `LocaleExtension` sets `Locale.US` globally.
- **Test ordering**: `TestClassOrderer` runs `*Test` first, `*IT` second.
- **OTel tests**: Import `OpenTelemetryTestConfiguration` to get `InMemorySpanExporter` bean. Also add `@AutoConfigureTracing` and `@AutoConfigureMetrics`.
- **SOAP integration tests**: Also import `WebServiceTemplateConfiguration` for JAXB marshaller. `@ActiveProfiles("local")` is required.
- **Slice tests**: `@WebServiceServerTest(CountrySoapEndpoint.class)` — must provide a `Jaxb2Marshaller` bean manually.
- **REST tests**: Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@AutoConfigureRestTestClient` (inject `RestTestClient`).
- **Camel route tests**: `@SpringBootTest` + `@ActiveProfiles("local")`, inject `ProducerTemplate` to call routes directly.
- **Context load IT**: Uses `@SpringBootTest(useMainMethod = ALWAYS)`.

## Domain model (XSD → JAXB)

The XSD is the single source of truth for all request/response types. JAXB generates them into `target/generated-sources/jaxb/` (package `ch.dboeckli.soap.service.producingwebservice.schema`) — this directory does not exist on a fresh checkout; run `./mvnw generate-sources` first.

@src/main/resources/META-INF/schemas/countriesWs.xsd

## CI/CD pipeline

Three workflows in `.github/workflows/`:

| Workflow | Trigger | Key detail |
|---|---|---|
| `maven-build.yml` | push/PR/schedule | Three jobs: `setup` → `build` (`mvn deploy`), `analyze` (`mvn verify -Dskip.start.stop.springboot=true -Dskip.docker.build=true` + Sonar), `Trigger-Deploy` |
| `deploy-and-test-cluster.yml` | `workflow_dispatch` | Deploys via Helm to Kind cluster, runs `helm test` |
| `release.yml` | `workflow_dispatch` | `mvn release:prepare release:perform`, requires `main`/`master` + SNAPSHOT version |

- `ci-cd` Maven profile activates via `env.GITHUB_ACTIONS=true`; `master-branch`/`main-branch` profiles set `docker.image.tag=latest`.
- Feature branches get a computed version via `set_maven_version.sh` script in `.github/workflows/scripts/`.

## Observability stack

`docker compose -f compose.yaml up -d` starts: OTel Collector, Jaeger, Zipkin, Elasticsearch, Kibana, APM Server. The `local` Spring profile wires the app to export traces/metrics.

## Lombok

- `config.stopBubbling = true` — stops searching parent dirs.
- `lombok.addLombokGeneratedAnnotation = true` — adds `@jakarta.annotation.Generated` to generated code so JaCoCo excludes it from coverage.

## External tools

- **context7** — when you need to look up library/API docs, run `npx context7 <query>` or `npx context7 <package> <topic>`. Also available as an MCP server (`https://mcp.context7.com/mcp`, optional API key via `CONTEXT7_API_KEY` env var) — usable in IntelliJ and other MCP-compatible editors.

## Non-obvious facts

- HTTP test requests use `{{application-port}}` from `httpRequests/http-client.env.json`.
- `SoapEndpointConfig` registers `CamelEndpointMapping` and `MessageEndpointAdapter` beans — these are required for the V3 `spring-ws:` Camel entry point to work alongside Spring-WS.
- The V3 route uses `interceptFrom("spring-ws:*")` to attach OTel span attributes for all Camel-managed SOAP endpoints.
- The `local` Spring profile must be active for `spring.webservices.wsdl-locations` and OTel export to be configured; without it the WSDL won't be served.
- Integration tests start/stop the Spring Boot app via the Failsafe/spring-boot-maven-plugin lifecycle on a dynamically reserved port (`tomcat.http.port`).
