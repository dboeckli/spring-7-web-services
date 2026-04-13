package ch.dboeckli.soap.service.producingwebservice.soap;

import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequest;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequestV2;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponse;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponseV2;
import ch.dboeckli.soap.service.producingwebservice.soap.config.OpenTelemetryTestConfiguration;
import ch.dboeckli.soap.service.producingwebservice.soap.config.WebServiceTemplateConfiguration;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ws.client.core.WebServiceTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.opentelemetry.api.GlobalOpenTelemetry.resetForTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "management.otlp.metrics.export.enabled=false" })
@Import({ WebServiceTemplateConfiguration.class, OpenTelemetryTestConfiguration.class })
@Slf4j
@AutoConfigureTracing
@AutoConfigureMetrics
@ActiveProfiles("local")
class CountrySoapEndpointWithOtelIT {

    @LocalServerPort
    private int port;

    @Autowired
    private InMemorySpanExporter spanExporter;

    // @Autowired
    // private InMemoryMetricExporter metricExporter;

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
    void testSendAndReceiveWithSpan(@Autowired WebServiceTemplateBuilder builder) {
        WebServiceTemplate template = builder.build();
        GetCountryRequest request = new GetCountryRequest();
        request.setName("Spain");

        GetCountryResponse response = (GetCountryResponse) template
            .marshalSendAndReceive("http://localhost:%d/services".formatted(port), request);
        assertThat(response.getCountry().getCapital()).isEqualTo("Madrid");

        // Warten bis Spans exportiert wurden
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            log.info("Collected {} spans:", spans.size());
            spans.forEach(span -> log.info("  Span: name='{}', attributes={}", span.getName(), span.getAttributes()));

            assertThat(spans).isNotEmpty();

            // Span mit soap.method finden
            SpanData soapSpan = spans.stream()
                .filter(s -> s.getAttributes().get(AttributeKey.stringKey("soap.method")) != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No span with soap.method found. Available spans: "
                        + spans.stream().map(SpanData::getName).toList()));

            assertThat(soapSpan.getAttributes().get(AttributeKey.stringKey("soap.method"))).isNotNull();
            assertThat(soapSpan.getAttributes().get(AttributeKey.stringKey("soap.endpoint"))).isNotNull();
        });
    }

    @Test
    void testV2SendAndReceiveWithSpan(@Autowired WebServiceTemplateBuilder builder) {
        WebServiceTemplate template = builder.build();
        GetCountryRequestV2 request = new GetCountryRequestV2();
        request.setName("Spain");

        GetCountryResponseV2 response = (GetCountryResponseV2) template
            .marshalSendAndReceive("http://localhost:%d/services".formatted(port), request);
        assertThat(response.getCountry().getCapital()).isEqualTo("Madrid");

        // Warten bis Spans exportiert wurden
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            log.info("Collected {} spans:", spans.size());
            spans.forEach(span -> log.info("  Span: name='{}', attributes={}", span.getName(), span.getAttributes()));

            assertThat(spans).isNotEmpty();

            // Span mit soap.method finden
            SpanData soapSpan = spans.stream()
                .filter(s -> s.getAttributes().get(AttributeKey.stringKey("soap.method")) != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No span with soap.method found. Available spans: "
                        + spans.stream().map(SpanData::getName).toList()));

            assertThat(soapSpan.getAttributes().get(AttributeKey.stringKey("soap.method"))).isNotNull();
            assertThat(soapSpan.getAttributes().get(AttributeKey.stringKey("soap.endpoint"))).isNotNull();
        });
    }

}
