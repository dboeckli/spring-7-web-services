package ch.dboeckli.soap.service.producingwebservice.camel;

import ch.dboeckli.soap.service.producingwebservice.CountryRepository;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequestV2;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponseV2;
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
        from(DIRECT_GET_COUNTRY).routeId("country-get")
            .log(LoggingLevel.INFO, "country-get", "# body before transform is: ${body}")
            .id("log-country-get")
            .process(exchange -> {
                Object body = exchange.getMessage().getBody();
                log.info("Received GetCountryRequestV2 request: {}", body);
                if (body instanceof GetCountryRequestV2 request) {
                    GetCountryResponseV2 response = new GetCountryResponseV2();
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
