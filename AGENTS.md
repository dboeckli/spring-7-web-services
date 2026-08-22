# AGENTS.md

See `CLAUDE.md` for full project overview, build/test/lint commands, architecture table, and non-obvious facts. This file adds complementary signal.

## Project identity

- **Artifact**: `ch.dboeckli.spring.soap.service:spring-7-web-services:0.0.1-SNAPSHOT`
- **Java 25**, Spring Boot 4.1.0, Camel Spring Boot 4.21.0
- **SCM**: `https://github.com/dboeckli/spring-7-web-services`

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

## Testing details

- **JUnit extension auto-detection** enabled (`junit-platform.properties`). `LocaleExtension` sets `Locale.US` globally.
- **Test ordering**: `TestClassOrderer` runs `*Test` first, `*IT` second.
- **OTel tests**: Import `OpenTelemetryTestConfiguration` to get `InMemorySpanExporter` bean. Also add `@AutoConfigureTracing` and `@AutoConfigureMetrics`.
- **SOAP integration tests**: Also import `WebServiceTemplateConfiguration` for JAXB marshaller. `@ActiveProfiles("local")` is required.
- **Slice tests**: `@WebServiceServerTest(CountrySoapEndpoint.class)` — must provide a `Jaxb2Marshaller` bean manually.
- **REST tests**: Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@AutoConfigureRestTestClient` (inject `RestTestClient`).
- **Camel route tests**: `@SpringBootTest` + `@ActiveProfiles("local")`, inject `ProducerTemplate` to call routes directly.
- **Context load IT**: Uses `@SpringBootTest(useMainMethod = ALWAYS)`.

## CI/CD pipeline

Three workflows in `.github/workflows/`:

| Workflow | Trigger | Key detail |
|---|---|---|
| `maven-build.yml` | push/PR/schedule | Three jobs: `setup` → `build` (`mvn deploy`), `analyze` (`mvn verify -Dskip.start.stop.springboot=true -Dskip.docker.build=true` + Sonar), `Trigger-Deploy` |
| `deploy-and-test-cluster.yml` | `workflow_dispatch` | Deploys via Helm to Kind cluster, runs `helm test` |
| `release.yml` | `workflow_dispatch` | `mvn release:prepare release:perform`, requires `main`/`master` + SNAPSHOT version |

- `ci-cd` Maven profile activates via `env.GITHUB_ACTIONS=true`; `master-branch`/`main-branch` profiles set `docker.image.tag=latest`.
- Feature branches get a computed version via `set_maven_version.sh` script in `.github/workflows/scripts/`.

## Build infrastructure

- **Maven wrapper** (`./mvnw`) — no local Maven install needed.
- **Spring Boot layers** enabled for Docker images.
- **JaCoCo** for coverage (XML report to `target/site/jacoco/`).
- **CycloneDX** for SBOM, **git-commit-id** for build metadata.
- **Docker image** build at `install` phase via `spring-boot-maven-plugin:build-image-no-fork` (uses Buildpacks, `BP_JVM_VERSION=25`).
- **Helm** processed in `install` phase: lint → template → dry-run → package.
- **POM sort** enforced by Spotless — alphabetically ordered within sections.

## Observability stack

`docker compose -f compose.yaml up -d` starts: OTel Collector, Jaeger, Zipkin, Elasticsearch, Kibana, APM Server. The `local` Spring profile wires the app to export traces/metrics.

## Lombok

- `config.stopBubbling = true` — stops searching parent dirs.
- `lombok.addLombokGeneratedAnnotation = true` — adds `@jakarta.annotation.Generated` to generated code so JaCoCo excludes it from coverage.

## External tools

- **context7** — when you need to look up library/API docs, run `npx context7 <query>` or `npx context7 <package> <topic>`. Also available as an MCP server (`https://mcp.context7.com/mcp`, optional API key via `CONTEXT7_API_KEY` env var) — usable in IntelliJ and other MCP-compatible editors.

## Formatting quirks

- `spring-javaformat` uses **spaces** (configured in `.springjavaformatconfig`).
- Build will **fail at `validate` phase** if formatting is wrong — always run both formatters before committing.
