package ch.dboeckli.soap.service.producingwebservice.soap;

import ch.dboeckli.soap.service.producingwebservice.camel.CountryCamelRoute;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequestV2;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponseV2;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.jspecify.annotations.NonNull;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
@Slf4j
public class CountrySoapEndpointV2 {

    private static final String NAMESPACE_URI = "https://spring.io/guides/gs-producing-web-service";

    private final ProducerTemplate producerTemplate;

    public CountrySoapEndpointV2(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getCountryRequestV2")
    @ResponsePayload
    public GetCountryResponseV2 getCountryV2(@RequestPayload @NonNull GetCountryRequestV2 request) {
        log.info("getCountryV2 request: {}", request);
        return producerTemplate.requestBody(CountryCamelRoute.DIRECT_GET_COUNTRY, request, GetCountryResponseV2.class);
    }

}
