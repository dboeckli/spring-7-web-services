# Bruno-Collection aus WSDL-Import (Vergleichsvariante)

Diese Collection ist **nicht** aus den `.http`-Dateien abgeleitet, sondern per
`bru import wsdl` aus den von Spring-WS generierten WSDLs entstanden. Sie dient nur dem
Vergleich mit `httpRequests/bruno/` (Ableitung aus den IntelliJ-Flows).

## Erzeugung (reproduzierbar)

1. Die App muss laufen (`local`-Profil). `WebServiceConfig` stellt drei WSDL-Definitionen bereit
   (`DefaultWsdl11Definition` mit eigenem `requestSuffix`, da der Default nur `*Request` erkennt):

   |     Bean      |                     WSDL-URL                      |    Request-Element    |
   |---------------|---------------------------------------------------|-----------------------|
   | `countries`   | `http://localhost:8080/services/countries.wsdl`   | `getCountryRequest`   |
   | `countriesV2` | `http://localhost:8080/services/countriesV2.wsdl` | `getCountryRequestV2` |
   | `countriesV3` | `http://localhost:8080/services/countriesV3.wsdl` | `getCountryRequestV3` |

2. WSDLs laden und importieren:

   ```bash
   curl -s -o /tmp/countries.wsdl   http://localhost:8080/services/countries.wsdl
   curl -s -o /tmp/countriesV2.wsdl http://localhost:8080/services/countriesV2.wsdl
   curl -s -o /tmp/countriesV3.wsdl http://localhost:8080/services/countriesV3.wsdl

   bru import wsdl -s /tmp/countries.wsdl   -o /tmp/imp-v1 -n "countries v1"
   bru import wsdl -s /tmp/countriesV2.wsdl -o /tmp/imp-v2 -n "countries v2"
   bru import wsdl -s /tmp/countriesV3.wsdl -o /tmp/imp-v3 -n "countries v3"
   ```
3. Import-Ergebnisse nach `v1/`, `v2/`, `v3/` kopieren; danach je Request die relative URL
   `/services` auf `http://localhost:{{application-port}}/services` setzen (der Importer übernimmt
   nur den Pfad aus `soap:address location`).

## Ergebnis des rohen Imports

- **Ein Request pro WSDL** (`getCountry`), Ablage unter `<PortType>Service/`.
- `POST`, `Content-Type: text/xml; charset=utf-8` und `SOAPAction: ""` sind korrekt.
- Body ist ein Beispiel-Envelope mit `<name>string</name>` — **ohne Target-Namespace** am
  Request-Element. Dadurch antwortet der Service mit `404` (SOAP-Fault „No endpoint mapping“).
- Keine `traceparent`/`baggage`-Header, keine Tests/Asserts, keine Flow-Reihenfolge,
  keine `httpexchanges`-Gegenprobe.
- `bru run v1` findet ohne `-r` keine Requests (Unterordner `CountriesPortService/`).

## Manuell nötig, um die Requests lauffähig zu machen

1. `xmlns="https://spring.io/guides/gs-producing-web-service"` am Request-Element ergänzen.
2. `<name>string</name>` durch einen echten Wert ersetzen (z. B. `Spain`).
3. Tests ergänzen (sonst meldet Bruno auch bei `404` „PASS“, weil keine Assertions laufen).

## Ausführen

```bash
cd httpRequests/bruno-wsdl
bru run v1 -r --env local
bru run v2 -r --env local
bru run v3 -r --env local
```

