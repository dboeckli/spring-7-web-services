# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose

This is a technical learning project for understanding web service patterns (SOAP, Apache Camel, OpenTelemetry). The business logic (`CountryRepository` with hardcoded country data) is intentionally trivial — it exists only as a payload for the technical plumbing. Do not suggest improvements to the business logic; focus exclusively on the technical integration patterns.

## Architecture

The project exposes three SOAP interface versions and one REST interface:

| Version   | Entry Point                                            | Flow                                                                                         |
| --------- | ------------------------------------------------------ | -------------------------------------------------------------------------------------------- |
| V1 (SOAP) | Spring-WS `@Endpoint` (`CountrySoapEndpoint`)          | SOAP Client → Spring-WS Endpoint → `CountryRepository`                                       |
| V2 (SOAP) | Spring-WS `@Endpoint` (`CountrySoapEndpointV2`)        | SOAP Client → Spring-WS Endpoint → Camel `direct:get-country` → `CountryRepository`          |
| V3 (SOAP) | Apache Camel `spring-ws:` component                    | SOAP Client → Camel `spring-ws:rootqname` → Camel `direct:get-country` → `CountryRepository` |
| REST      | Spring MVC `@RestController` (`CountryRestController`) | HTTP Client → `GET /api/countries/{name}` → `CountryRepository`                              |

Key design decisions:

- V2 and V3 share `direct:get-country` to avoid duplicating business logic. V1 and REST access `CountryRepository` directly.
- V2 uses Spring-WS as the HTTP entry point and delegates to Camel via `ProducerTemplate`; V3 lets Camel own the HTTP entry point via the `spring-ws:` component.
- `direct:get-country` handles JAXB marshalling/unmarshalling and OpenTelemetry baggage propagation for both V2 and V3.

## Build & Run

```bash
./mvnw clean install                                        # full build (includes tests, Helm, Docker image)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local    # run locally with observability config
./mvnw generate-sources                                     # regenerate JAXB classes from XSD
docker compose -f compose.yaml up -d                       # start observability stack (OTel, Elasticsearch, Kibana, APM)
```

## Testing

Tests follow Maven naming conventions: `*Test` runs via Surefire (unit/slice), `*IT` runs via Failsafe (integration).

```bash
./mvnw test                                           # unit and slice tests only
./mvnw verify                                         # all tests including integration tests
./mvnw test -Dtest=CountrySoapEndpointSliceTest       # single unit/slice test
./mvnw failsafe:integration-test -Dit.test=CountrySoapEndpointIT  # single integration test
```

- `OpenTelemetryTestConfiguration` — shared OTel test config that replaces the real exporter with in-memory spans; include it in OTel-related tests.

## Code Formatting

Two formatters are enforced at the `validate` phase and will fail the build:

```bash
./mvnw spring-javaformat:apply   # fix Java code style (Spring conventions)
./mvnw spotless:apply            # fix POM, Markdown, JSON, YAML, shell scripts
```

Always run both before committing. `spring-javaformat` applies Spring's Java style; `spotless` handles everything else including POM sort order.

## Domain model (XSD → JAXB)

The XSD is the single source of truth for all request/response types. JAXB generates them into
`target/generated-sources/jaxb/` (package `ch.dboeckli.soap.service.producingwebservice.schema`) —
this directory does not exist on a fresh checkout; run `./mvnw generate-sources` first.

@src/main/resources/META-INF/schemas/countriesWs.xsd

## Non-obvious facts

- HTTP test requests use `{{application-port}}` from `httpRequests/http-client.env.json`.
- `SoapEndpointConfig` registers `CamelEndpointMapping` and `MessageEndpointAdapter` beans — these are required for the V3 `spring-ws:` Camel entry point to work alongside Spring-WS.
- The V3 route uses `interceptFrom("spring-ws:*")` to attach OTel span attributes for all Camel-managed SOAP endpoints.
- The `local` Spring profile must be active for `spring.webservices.wsdl-locations` and OTel export to be configured; without it the WSDL won't be served.
- Integration tests start/stop the Spring Boot app via the Failsafe/spring-boot-maven-plugin lifecycle on a dynamically reserved port (`tomcat.http.port`).
