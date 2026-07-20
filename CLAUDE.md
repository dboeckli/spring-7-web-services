# CLAUDE.md — spring-7-web-services

@README.adoc

## Build & Run

```bash
./mvnw spring-boot:run          # start the application
./mvnw generate-sources         # regenerate JAXB classes from XSD (run when missing)
docker compose -f compose.yaml up -d   # start observability stack (OTel collector, etc.)
```

## Non-obvious facts

- JAXB sources are generated into `target/generated-sources/jaxb/` — IDE may report compile errors until `generate-sources` has run.
- HTTP test requests use `{{application-port}}` from `httpRequests/http-client.env.json`.
- The shared Camel route `direct:get-country` is the only route that touches `CountryRepository` for SOAP V2 and V3.

## Skills

- `/camel-matrix [minVersion maxVersion]` — generate `camel-springboot-matrix.adoc` with Camel / Spring Boot / CXF version compatibility data scraped from Maven Central.

