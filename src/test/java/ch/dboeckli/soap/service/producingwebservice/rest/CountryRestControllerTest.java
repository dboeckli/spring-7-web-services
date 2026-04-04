package ch.dboeckli.soap.service.producingwebservice.rest;

import ch.dboeckli.soap.service.producingwebservice.schema.Country;
import ch.dboeckli.soap.service.producingwebservice.schema.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("local")
class CountryRestControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void getCountryReturnsExpectedCountry() {
        Country result = restTestClient.get()
            .uri("/api/countries/{name}", "Spain")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody(Country.class)
            .returnResult()
            .getResponseBody();

        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result.getName()).isEqualTo("Spain"),
                () -> assertThat(result.getCapital()).isEqualTo("Madrid"),
                () -> assertThat(result.getCurrency()).isEqualTo(Currency.EUR),
                () -> assertThat(result.getPopulation()).isEqualTo(46_704_314));
    }

}
