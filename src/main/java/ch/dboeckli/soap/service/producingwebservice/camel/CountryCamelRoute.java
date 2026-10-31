package ch.dboeckli.soap.service.producingwebservice.camel;

import ch.dboeckli.soap.service.producingwebservice.CountryRepository;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequestV2;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequestV3;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequestV4;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponseV2;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponseV3;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponseV4;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class CountryCamelRoute extends RouteBuilder {

    public static final String DIRECT_GET_COUNTRY = "direct:get-country";

    private final CountryRepository countryRepository;

    public CountryCamelRoute(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @Override
    public void configure() {
        Processor soapEndpointTracing = exchange -> {
            Span currentSpan = Span.current();
            if (currentSpan != null && currentSpan.isRecording()) {
                String endpointUri = exchange.getFromEndpoint().getEndpointUri();
                currentSpan.setAttribute("soap.endpoint", endpointUri);
                if (endpointUri.startsWith("spring-ws:")) {
                    String qname = endpointUri.replace("spring-ws:rootqname:", "").split("\\?")[0];
                    currentSpan.setAttribute("soap.qname", qname);
                }
            }
        };
        interceptFrom("spring-ws:*").process(soapEndpointTracing);
        interceptFrom("cxf:*").process(soapEndpointTracing);

        // V3 SOAP-Eingang via Camel – technischer Test, nutzt V2-Objekte
        from("spring-ws:rootqname:{https://spring.io/guides/gs-producing-web-service}getCountryRequestV3"
                + "?endpointMapping=#endpointMapping")
            .routeId("soap-get-country-v3-camel")
            .log(LoggingLevel.INFO, "country-get", "# V3 (Camel) SOAP request received")
            // Convert the Spring-WS Source to String first: Woodstox's DOM wrapping
            // reader does not intern QName parts, which the JAXB RI rejects.
            .convertBodyTo(String.class)
            .unmarshal()
            .jaxb("ch.dboeckli.soap.service.producingwebservice.schema")
            .to(DIRECT_GET_COUNTRY)
            .marshal()
            .jaxb("ch.dboeckli.soap.service.producingwebservice.schema");

        // V4 SOAP-Eingang via Camel-CXF – CXF liefert Transport und WSDL, Camel bleibt
        // Endpoint-Owner
        from("cxf:/countriesV4?serviceClass=ch.dboeckli.soap.service.producingwebservice.camel.CountryServiceV4"
                + "&wsdlURL=classpath:wsdl/countriesV4.wsdl&dataFormat=POJO")
            .routeId("soap-get-country-v4-cxf")
            .log(LoggingLevel.INFO, "country-get", "# V4 (Camel-CXF) SOAP request received")
            .process(exchange -> {
                List<?> parameters = exchange.getMessage().getBody(List.class);
                exchange.getMessage().setBody(parameters.get(0));
            })
            .to(DIRECT_GET_COUNTRY);

        from(DIRECT_GET_COUNTRY).routeId(DIRECT_GET_COUNTRY)
            .setProperty("CamelBaggage_myValue", constant("1234"))
            .log(LoggingLevel.INFO, "country-get", "# body before transform is: ${body}")
            .id("log-country-get")

            .process(exchange -> {
                // Baggage is available via the OpenTelemetry API
                String val = Baggage.current().getEntryValue("myValue");
                log.info("Baggage value: {}", val);
            })
            .to("log:info")

            .process(exchange -> {
                Object body = exchange.getMessage().getBody();
                log.info("Received GetCountryRequest request: {}", body);
                if (body instanceof GetCountryRequestV2 request) {
                    GetCountryResponseV2 response = new GetCountryResponseV2();
                    response.setCountry(countryRepository.findCountry(request.getName()));
                    exchange.getMessage().setBody(response);
                    return;
                }
                else if (body instanceof GetCountryRequestV3 request) {
                    GetCountryResponseV3 response = new GetCountryResponseV3();
                    response.setCountry(countryRepository.findCountry(request.getName()));
                    exchange.getMessage().setBody(response);
                    return;
                }
                else if (body instanceof GetCountryRequestV4 request) {
                    GetCountryResponseV4 response = new GetCountryResponseV4();
                    response.setCountry(countryRepository.findCountry(request.getName()));
                    exchange.getMessage().setBody(response);
                    return;
                }
                throw new IllegalArgumentException(
                        "Unsupported request type: " + (body == null ? "null" : body.getClass().getName()));
            })
            .id("process-exchange");
    }

}
