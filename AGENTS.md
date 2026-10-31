# AGENTS.md

## Purpose

This is a technical learning project for understanding web service patterns (SOAP, Apache Camel, OpenTelemetry). The business logic (`CountryRepository` with hardcoded country data) is intentionally trivial — it exists only as a payload for the technical plumbing. Do not suggest improvements to the business logic; focus exclusively on the technical integration patterns.

## Project identity

- **Artifact**: `ch.dboeckli.spring.soap.service:spring-7-web-services:0.0.1-SNAPSHOT`
- **Java 25**, Spring Boot 4.1.2, Camel Spring Boot 4.23.0
- **SCM**: `https://github.com/dboeckli/spring-7-web-services`

## Architecture

The project exposes four SOAP interface versions and one REST interface:

| Version    | Entry Point                                            | Flow                                                                                         |
| ---------- | ------------------------------------------------------ | -------------------------------------------------------------------------------------------- |
| V1 (SOAP)  | Spring-WS `@Endpoint` (`CountrySoapEndpoint`)          | SOAP Client → Spring-WS Endpoint → `CountryRepository`                                       |
| V2 (SOAP)  | Spring-WS `@Endpoint` (`CountrySoapEndpointV2`)        | SOAP Client → Spring-WS Endpoint → Camel `direct:get-country` → `CountryRepository`          |
| V3 (SOAP)  | Apache Camel `spring-ws:` component                    | SOAP Client → Camel `spring-ws:rootqname` → Camel `direct:get-country` → `CountryRepository` |
| V4 (SOAP)  | Apache Camel `cxf:` component (`CountryServiceV4` SEI) | SOAP Client → `POST /cxf/countriesV4` → Camel `direct:get-country` → `CountryRepository`    |
| REST       | Spring MVC `@RestController` (`CountryRestController`) | HTTP Client → `GET /api/countries/{name}` → `CountryRepository`                              |

Key design decisions:

- V2, V3 and V4 share `direct:get-country` to avoid duplicating business logic. V1 and REST access `CountryRepository` directly.
- V2 uses Spring-WS as the HTTP entry point and delegates to Camel via `ProducerTemplate`; V3 lets Camel own the HTTP entry point via the `spring-ws:` component.
- V4 uses Camel's `cxf:` component (POJO mode with the `CountryServiceV4` JAX-WS SEI): Camel stays the endpoint owner, CXF provides transport, WSDL (`/cxf/countriesV4?wsdl`) and the `/cxf/` overview page.
- `direct:get-country` handles JAXB marshalling/unmarshalling and OpenTelemetry baggage propagation for V2, V3 and V4.

## Package layout (main)

| Package | Contents |
|---|---|
| `ch.dboeckli.soap.service` | `SpringSoapServiceApplication` |
| `…producingwebservice` | `CountryRepository` |
| `…producingwebservice.soap` | `CountrySoapEndpoint` (V1), `CountrySoapEndpointV2` (V2) |
| `…producingwebservice.soap.config` | `WebServiceConfig` (WSDL bean), `CustomEndpointInterceptor` |
| `…producingwebservice.camel` | `CountryCamelRoute` (V3/V4 entries + `direct:get-country`), `CountryServiceV4` (JAX-WS SEI), `SoapEndpointConfig` (V3 beans + sync CXFServlet) |
| `…producingwebservice.rest` | `CountryRestController` (`GET /api/countries/{name}`) |
| `ch.dboeckli.soap.service.tracing` | `BaggageTaggingFilter`, `PayloadLoggingFilter`, `TraceDebugFilter`, `TraceParentFilter`, `TracingHttpExchangeRepository` |
| `ch.dboeckli.soap.service.log` | `RequestLoggingConfig`, `RequestLoggingFilter` (request logging) |

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
- **Helm** processed in `install` phase: dependencies → lint → template → dry-run → package.
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
| `maven-build.yml` | push/PR/schedule | Three jobs: `setup` → `build` (`mvn deploy`), `analyze` (`mvn verify -Dskip.start.stop.springboot=true -Dskip.docker.build=true -Dskip.docker.publish=true` + Sonar), `Trigger-Deploy` |
| `deploy-and-test-cluster.yml` | `workflow_dispatch` | Deploys via Helm to Kind cluster, runs `helm test` |
| `release.yml` | `workflow_dispatch` | `mvn release:prepare release:perform`, requires `main`/`master` + SNAPSHOT version |

- `ci-cd` Maven profile activates via `env.GITHUB_ACTIONS=true`; the `master-branch`/`main-branch` profiles set `docker.image.tag=snapshot`, the `release` profile (`performRelease`) sets `latest`.
- Feature branches get a computed version via `set_maven_version.sh` script in `.github/workflows/scripts/`.

## Observability stack

`docker compose -f compose.yaml up -d` starts: OTel Collector, Jaeger, Zipkin, Elasticsearch, Kibana, APM Server. The `local` Spring profile wires the app to export traces/metrics.

## Lombok

- `config.stopBubbling = true` — stops searching parent dirs.
- `lombok.addLombokGeneratedAnnotation = true` — adds `@jakarta.annotation.Generated` to generated code so JaCoCo excludes it from coverage.

## External tools

- **context7** — when you need to look up library/API docs, run `npx ctx7 library "<name>" "<topic>"` to find the library ID, then `npx ctx7 docs <libraryId> <query>`. An MCP server (`https://mcp.context7.com/mcp`, optional API key via `CONTEXT7_API_KEY`) is also available for MCP-compatible editors.

## Non-obvious facts

- HTTP test requests use `{{application-port}}` from `httpRequests/http-client.env.json`.
- IntelliJ-HTTP-Flows: `httpRequests/restRequest/country-flow/` (REST) and `httpRequests/soapRequest/country-versions-flow/` (V1/V2/V3). Each flow ships a `.md` with a Mermaid diagram; steps build on each other via `client.global` (run in order). SOAP envelopes stay external XML files, referenced via `< ../getCountryRequest*.xml`.
- `SoapEndpointConfig` registers `CamelEndpointMapping` and `MessageEndpointAdapter` beans — these are required for the V3 `spring-ws:` Camel entry point to work alongside Spring-WS. It also registers the `CXFServlet` synchronously (`setAsyncSupported(false)`) so the caching `PayloadLoggingFilter` does not lose CXF async responses.
- V4 uses `cxf.path=/cxf` (Spring-WS keeps `/services`) and the WSDL at `src/main/resources/wsdl/countriesV4.wsdl`; CXF adds Woodstox, so the V3 route calls `convertBodyTo(String.class)` before `.unmarshal().jaxb(...)` because Woodstox's DOM wrapping reader does not intern QName parts.
- The route uses `interceptFrom("spring-ws:*,cxf:*")` to attach OTel span attributes for all Camel-managed SOAP endpoints.
- The `local` Spring profile must be active for `spring.webservices.wsdl-locations` and OTel export to be configured; without it the WSDL won't be served.
- Integration tests start/stop the Spring Boot app via the Failsafe/spring-boot-maven-plugin lifecycle on a dynamically reserved port (`tomcat.http.port`).
