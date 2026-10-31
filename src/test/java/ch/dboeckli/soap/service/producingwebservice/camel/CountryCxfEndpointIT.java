package ch.dboeckli.soap.service.producingwebservice.camel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "management.otlp.metrics.export.enabled=false" })
@AutoConfigureRestTestClient
@ActiveProfiles("local")
class CountryCxfEndpointIT {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void serviceListPageListsV4Endpoint() {
        String body = restTestClient.get()
            .uri("/cxf/")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(body).contains("countriesV4");
    }

    @Test
    void soapRequestReturnsCountry() {
        String request = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:gs="https://spring.io/guides/gs-producing-web-service">
                    <soapenv:Header/>
                    <soapenv:Body>
                        <gs:getCountryRequestV4>
                            <gs:name>Spain</gs:name>
                        </gs:getCountryRequestV4>
                    </soapenv:Body>
                </soapenv:Envelope>
                """;

        String body = restTestClient.post()
            .uri("/cxf/countriesV4")
            .contentType(MediaType.TEXT_XML)
            .header("SOAPAction", "\"\"")
            .body(request)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(body).contains("getCountryResponseV4");
        assertThat(body).contains("<name>Spain</name>");
        assertThat(body).contains("<capital>Madrid</capital>");
    }

    @Test
    void wsdlIsServed() {
        String body = restTestClient.get()
            .uri("/cxf/countriesV4?wsdl")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(body).contains("CountriesPortV4");
        assertThat(body).contains("getCountryRequestV4");
    }

}
