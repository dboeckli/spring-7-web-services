package ch.dboeckli.soap.service.producingwebservice.rest;

import ch.dboeckli.soap.service.producingwebservice.common.config.OpenTelemetryTestConfiguration;
import ch.dboeckli.soap.service.producingwebservice.schema.Country;
import ch.dboeckli.soap.service.producingwebservice.schema.Currency;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import static io.opentelemetry.api.GlobalOpenTelemetry.resetForTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "management.otlp.metrics.export.enabled=false", "spring.docker.compose.skip.in-tests=false" })
@Import({ OpenTelemetryTestConfiguration.class })
@AutoConfigureRestTestClient
@AutoConfigureTracing
@AutoConfigureMetrics
@ActiveProfiles("local")
class CountryRestControllerWithOtelIT {

    @LocalServerPort
    private int port;

    @Autowired
    private InMemorySpanExporter spanExporter;

    @Autowired
    private RestTestClient restTestClient;

    @BeforeEach
    void setUp() {
        spanExporter.reset();

    }

    @AfterEach
    void tearDown() {
        spanExporter.reset();
        resetForTest();
    }

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
