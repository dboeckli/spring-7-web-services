# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@README.adoc

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

## Non-obvious facts

- JAXB sources are generated into `target/generated-sources/jaxb/` under package `ch.dboeckli.soap.service.producingwebservice.schema` — IDEs report compile errors until `generate-sources` has run.
- HTTP test requests use `{{application-port}}` from `httpRequests/http-client.env.json`.
- `SoapEndpointConfig` registers `CamelEndpointMapping` and `MessageEndpointAdapter` beans — these are required for the V3 `spring-ws:` Camel entry point to work alongside Spring-WS.
- The V3 route uses `interceptFrom("spring-ws:*")` to attach OTel span attributes for all Camel-managed SOAP endpoints.
- The `local` Spring profile must be active for `spring.webservices.wsdl-locations` and OTel export to be configured; without it the WSDL won't be served.
- Integration tests start/stop the Spring Boot app via the Failsafe/spring-boot-maven-plugin lifecycle on a dynamically reserved port (`tomcat.http.port`).

## Architecture

Key design decision: V2 and V3 share `direct:get-country` to avoid duplicating business logic. V2 uses Spring-WS as the HTTP entry point and delegates to Camel via `ProducerTemplate`; V3 lets Camel own the HTTP entry point via the `spring-ws:` component.
