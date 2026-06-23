package ch.dboeckli.soap.service.producingwebservice.camel;

import ch.dboeckli.soap.service.producingwebservice.CountryRepository;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequestV2;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequestV3;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponseV2;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponseV3;
import io.opentelemetry.api.baggage.Baggage;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

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
        // V3 SOAP-Eingang via Camel – technischer Test, nutzt V2-Objekte
        from("spring-ws:rootqname:{https://spring.io/guides/gs-producing-web-service}getCountryRequestV3"
                + "?endpointMapping=#endpointMapping")
            .routeId("soap-get-country-v3-camel")
            .log(LoggingLevel.INFO, "country-get", "# V3 (Camel) SOAP request received")
            .unmarshal()
            .jaxb("ch.dboeckli.soap.service.producingwebservice.schema")
            .to(DIRECT_GET_COUNTRY)
            .marshal()
            .jaxb("ch.dboeckli.soap.service.producingwebservice.schema");

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
                log.info("Received GetCountryRequestV2 request: {}", body);
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
                throw new IllegalArgumentException(
                        "Unsupported request type: " + (body == null ? "null" : body.getClass().getName()));
            })
            .id("process-exchange");
    }

}
