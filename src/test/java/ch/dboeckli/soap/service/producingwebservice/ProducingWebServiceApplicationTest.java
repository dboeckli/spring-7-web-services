package ch.dboeckli.soap.service.producingwebservice;

import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequest;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponse;
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
public class ProducingWebServiceApplicationTest {

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

}
