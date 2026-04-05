package ch.dboeckli.soap.service.producingwebservice.soap;

import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequest;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequestV2;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponse;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponseV2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ws.client.core.WebServiceTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(WebServiceTemplateConfiguration.class)
@ActiveProfiles("local")
class CountrySoapEndpointTest {

    @LocalServerPort
    private int port;

    @Test
    public void testSendAndReceive(@Autowired WebServiceTemplateBuilder builder) {
        WebServiceTemplate template = builder.build();
        GetCountryRequest request = new GetCountryRequest();
        request.setName("Spain");

        GetCountryResponse response = (GetCountryResponse) template
            .marshalSendAndReceive("http://localhost:%d/services".formatted(port), request);
        assertThat(response.getCountry().getCapital()).isEqualTo("Madrid");
    }

    @Test
    public void testV2SendAndReceive(@Autowired WebServiceTemplateBuilder builder) {
        WebServiceTemplate template = builder.build();
        GetCountryRequestV2 request = new GetCountryRequestV2();
        request.setName("Spain");

        GetCountryResponseV2 response = (GetCountryResponseV2) template
            .marshalSendAndReceive("http://localhost:%d/services".formatted(port), request);
        assertThat(response.getCountry().getCapital()).isEqualTo("Madrid");
    }

}
