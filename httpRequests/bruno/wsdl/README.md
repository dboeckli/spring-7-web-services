# SOAP-Contract (WSDLs) + generierte Requests

Die XSD unter `src/main/resources/META-INF/schemas/countriesWs.xsd` ist die Single Source of
Truth. Zur Laufzeit generiert Spring-WS daraus WSDLs (`WebServiceConfig`); dieser Ordner enthält
die **Snapshots** des Contracts für V1–V4 und die daraus per `bru import wsdl` erzeugten
Requests.

|       Datei        |                     WSDL-URL                      |     Port-Type     |    Request-Element    |
|--------------------|---------------------------------------------------|-------------------|-----------------------|
| `countries.wsdl`   | `http://localhost:8080/services/countries.wsdl`   | `CountriesPort`   | `getCountryRequest`   |
| `countriesV2.wsdl` | `http://localhost:8080/services/countriesV2.wsdl` | `CountriesPortV2` | `getCountryRequestV2` |
| `countriesV3.wsdl` | `http://localhost:8080/services/countriesV3.wsdl` | `CountriesPortV3` | `getCountryRequestV3` |
| `countriesV4.wsdl` | `http://localhost:8080/cxf/countriesV4?wsdl`      | `CountriesPortV4` | `getCountryRequestV4` |

Vier WSDL-Definitionen sind nötig, weil `DefaultWsdl11Definition` per Default nur Elemente mit
Suffix `Request` als Operation erkennt. V2/V3 setzen deshalb eigene `requestSuffix`/`responseSuffix`
(`RequestV2`/`ResponseV2`, `RequestV3`/`ResponseV3`). V4 ist ein statisches WSDL für den
Camel-CXF-Endpoint (`src/main/resources/wsdl/countriesV4.wsdl`).

V1–V3 zeigen mit `soap:address location="/services"` auf denselben Endpoint; die Version
steckt im Envelope-Element. V4 wird unter `/cxf/countriesV4` veröffentlicht.

## Generierte Requests

`CountriesPortService/`, `CountriesPortV2Service/`, `CountriesPortV3Service/` und
`CountriesPortV4Service/` werden mit `tools/import-wsdl.mjs` erzeugt (Wrapper um
`bru import wsdl`). Der Wrapper korrigiert zwei Dinge, die der Importer für diesen Service falsch
macht:

1. Relative/absolute `soap:address`-URL → `http://localhost:{{application-port}}<pfad>`
   (`/services` bzw. `/cxf/countriesV4`)
2. Target-Namespace am Request-Element ergänzen (sonst antwortet der Service mit `404`)

```bash
cd httpRequests/bruno
node tools/import-wsdl.mjs
bru run wsdl -r --env local
```

Die Requests sind bewusst **minimal**: Beispiel-Body mit `<name>string</name>`, keine
`traceparent`/`baggage`-Header, nur ein generierter `200`-Check. Für echte Countries (`Spain`,
`Poland`, `United Kingdom`) den `name` anpassen; für Trace-Prüfungen und Asserts den `.http`-Flow
(`soapRequest/country-versions-flow`) bzw. dessen Bruno-Ableitung verwenden.

## WSDL-Snapshots aktualisieren

App mit `local`-Profil starten, dann aus dem Repo-Root:

```bash
for name in countries countriesV2 countriesV3; do
  curl -fsS "http://localhost:8080/services/${name}.wsdl" \
    -o "httpRequests/bruno/wsdl/${name}.wsdl"
done
curl -fsS "http://localhost:8080/cxf/countriesV4?wsdl" \
  -o "httpRequests/bruno/wsdl/countriesV4.wsdl"
node httpRequests/bruno/tools/import-wsdl.mjs
```

Nach XSD-Änderungen (`./mvnw generate-sources`) die WSDLs neu ziehen und die Requests neu
generieren. Die Dateien sind Momentaufnahmen — bei Bedarf mit `git diff` prüfen.
