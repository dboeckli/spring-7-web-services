package ch.dboeckli.soap.service.producingwebservice.camel;

import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequest;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequestV2;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponse;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponseV2;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
class CountryCamelRouteTest {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Test
    void shouldRejectV1Request() {
        GetCountryRequest request = new GetCountryRequest();
        request.setName("Spain");

        assertThatThrownBy(() -> producerTemplate.requestBody(CountryCamelRoute.DIRECT_GET_COUNTRY, request,
                GetCountryResponse.class))
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Unsupported request type: " + request.getClass().getName());
    }

    @Test
    void shouldReturnCountryForV2Request() {
        GetCountryRequestV2 request = new GetCountryRequestV2();
        request.setName("Spain");

        GetCountryResponseV2 response = producerTemplate.requestBody(CountryCamelRoute.DIRECT_GET_COUNTRY, request,
                GetCountryResponseV2.class);

        assertThat(response.getCountry().getCapital()).isEqualTo("Madrid");
    }

}
