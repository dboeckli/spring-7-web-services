# SOAP-Flow: Country-Lookup über V1 / V2 / V3 / V4

Datei: `httpRequests/soapRequest/country-versions-flow/country-versions-flow.http`

## Ziel

Die vier SOAP-Integrationspfade mit identischem Payload vergleichen und dabei die
Trace-Propagation prüfen:

- **V1** – Spring-WS `@Endpoint` (`CountrySoapEndpoint`), direkt zum `CountryRepository`.
- **V2** – Spring-WS `@Endpoint` (`CountrySoapEndpointV2`), delegiert per `ProducerTemplate` an
  Camel `direct:get-country`.
- **V3** – Camel `spring-ws:` als HTTP-Einstiegspunkt, direkt in `direct:get-country`.
- **V4** – Camel-CXF (`cxf:`) als HTTP-Einstiegspunkt (`/cxf/countriesV4`), direkt in
  `direct:get-country`; CXF liefert WSDL und Services-Übersicht.

Jede Variante wird einmal mit eingehendem `traceparent` und einmal ohne (Server erzeugt einen
neuen Trace) aufgerufen. V1–V3 laufen über denselben Endpoint `POST /services` und
unterscheiden sich nur im Envelope-Element; V4 läuft über `POST /cxf/countriesV4`.

## Ablauf

```mermaid
sequenceDiagram
    actor C as HTTP Client (IntelliJ)
    participant APP as spring-7-web-services
    participant CAMEL as Camel direct:get-country
    participant REPO as CountryRepository

    C->>APP: GET /actuator/health
    APP-->>C: 200 UP

    Note over C,REPO: V1 - Spring-WS @Endpoint
    C->>APP: POST /services (getCountryRequest, traceparent)
    APP->>REPO: findCountry("Spain")
    REPO-->>APP: Country(Spain)
    APP-->>C: 200 getCountryResponse

    Note over C,REPO: V2 - Spring-WS @Endpoint -> Camel
    C->>APP: POST /services (getCountryRequestV2, traceparent)
    APP->>CAMEL: ProducerTemplate
    CAMEL->>REPO: findCountry("Spain")
    REPO-->>CAMEL: Country(Spain)
    CAMEL-->>APP: Country(Spain)
    APP-->>C: 200 getCountryResponseV2

    Note over C,REPO: V3 - Camel spring-ws:
    C->>APP: POST /services (getCountryRequestV3, traceparent)
    APP->>CAMEL: spring-ws consumer
    CAMEL->>REPO: findCountry("Spain")
    REPO-->>CAMEL: Country(Spain)
    CAMEL-->>C: 200 getCountryResponseV3 + SOAP-Header (traceparent, baggage)

    Note over C,REPO: V4 - Camel-CXF
    C->>APP: POST /cxf/countriesV4 (getCountryRequestV4, traceparent)
    APP->>CAMEL: cxf: consumer
    CAMEL->>REPO: findCountry("Spain")
    REPO-->>CAMEL: Country(Spain)
    CAMEL-->>C: 200 getCountryResponseV4

    C->>APP: GET /actuator/httpexchanges
    APP-->>C: 200 (vier Exchanges mit demselben traceId)
```

## Requests (in dieser Reihenfolge)

1. `health` – `GET /actuator/health`; Vorbedingung, `status=UP`.
2. `SOAP V1 with an explicit traceparent` – setzt `traceId`/`spanId`, Body
   `../getCountryRequest.xml`.
3. `SOAP V1 without traceparent` – Server-Trace, Response-Header `traceparent`.
4. `SOAP V2 with traceparent` – neuer `spanId`, gleicher `traceId`; Body
   `../getCountryRequestV2.xml`.
5. `SOAP V2 without traceparent`.
6. `SOAP V3 with traceparent` – neuer `spanId`, gleicher `traceId`; Body
   `../getCountryRequestV3.xml`.
7. `SOAP V3 without traceparent`.
8. `SOAP V4 with traceparent` – neuer `spanId`, gleicher `traceId`; Body
   `../getCountryRequestV4.xml`; `POST /cxf/countriesV4`.
9. `SOAP V4 without traceparent`.
10. `verify the recorded V1/V2/V3/V4 exchanges` – `GET /actuator/httpexchanges`; prüft, dass genau
    vier Exchanges (drei `/services` + ein `/cxf/countriesV4`) den `traceId` aus Schritt 2 tragen.

## Benötigte Globals

|  Global   |      gesetzt von       |      genutzt von       |
|-----------|------------------------|------------------------|
| `traceId` | Schritt 2 (Pre-Script) | Schritt 2, 4, 6, 8, 10 |
| `spanId`  | Schritt 2, 4, 6, 8     | jeweiliger Request     |

## Asserts

- Health → `200`, `status=UP`.
- V1/V2/V3 mit `traceparent` → `200`, jeweils eigenes Response-Element
  (`getCountryResponse` / `...V2` / `...V3`), `name=Spain`, `capital=Madrid`, `currency=EUR`;
  **kein** HTTP-Response-`traceparent` (eingehender Trace wird nicht überschrieben).
- V3 zusätzlich → SOAP-Header enthält `traceparent="00-<traceId>-…"` und `baggage="…"`.
- V4 mit `traceparent` → `200`, `getCountryResponseV4`, `name=Spain`, `capital=Madrid`,
  `currency=EUR`; der HTTP-Response-`traceparent` trägt dieselbe `traceId` (V4 propagiert den
  Header, anders als V1–V3).
- V1/V2/V3/V4 ohne `traceparent` → `200`, `name=Spain`, HTTP-Response-`traceparent` im Format
  `00-<32 hex>-<16 hex>-<2 hex flags>` (V1–V3 `-01`, V4 `-03`).
- `httpexchanges` → `200`, genau vier Exchanges (drei `/services`, ein `/cxf/countriesV4`) mit dem
  `traceId` aus Schritt 2, alle mit Status `200`.

## Body-Strategie (Entscheidung für IntelliJ)

Die SOAP-Envelopes bleiben **externe XML-Dateien** (`../getCountryRequest*.xml`) und werden per
`< ../datei.xml` eingebunden:

- ein Envelope pro Version als Single Source of Truth, keine mehrfache Inline-Pflege in den
  Requests;
- die bestehenden XML-Dateien bleiben unverändert und werden von den Einzelrequest-Dateien
  weiterverwendet;
- die Flow-Datei liegt im Unterordner, daher `../` als relativer Pfad zum `soapRequest/`-Ordner.

Für Bruno (Phase 2) ist diese Variante nicht direkt nutzbar, weil Bruno keine Datei-Referenzen im
Body kennt — dort wird der Body beim Generieren inline eingebettet.

## Hinweise

- Basis-URL ist `{{application-port}}` (`local` = 8080, `k8s` = 30080).
- Der Flow benötigt das Spring-Profil `local` (WSDL unter `/services` und `/cxf`, `httpexchanges`-
  Recording, OTel-Propagation W3C).
- `SOAPAction: ""` und `Content-Type: text/xml; charset=utf-8` sind für alle Varianten identisch.
- Schritte 4, 6, 8 und 10 setzen den `traceId` aus Schritt 2 voraus (Guard im Pre-Script).
- Der V3-SOAP-Header (`traceparent`, `baggage`) kommt vom Camel-`spring-ws:`-Consumer; V4
  (Camel-CXF/POJO) liefert keinen SOAP-Header, aber einen HTTP-Response-`traceparent`.

