package ch.dboeckli.soap.service.tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class PayloadLoggingFilter extends OncePerRequestFilter {

    private static final int CACHE_LIMIT = 1024 * 64; // 64 KB

    private final ObservationRegistry observationRegistry;

    public PayloadLoggingFilter(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper reqWrapper = new ContentCachingRequestWrapper(request, CACHE_LIMIT);
        ContentCachingResponseWrapper resWrapper = new ContentCachingResponseWrapper(response);

        filterChain.doFilter(reqWrapper, resWrapper);

        String requestBody = new String(reqWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        String responseBody = new String(resWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);

        String sanitizedRequestBody = requestBody.replaceAll("(\"password\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"")
            .replaceAll("(\"token\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");

        log.info("Request  Payload: {}", requestBody);
        log.info("Response Payload: {}", responseBody);

        Observation observation = observationRegistry.getCurrentObservation();
        if (observation != null) {
            observation.lowCardinalityKeyValue("http.request.body", truncate(sanitizedRequestBody, 1000));
            observation.lowCardinalityKeyValue("http.response.body", truncate(responseBody, 1000));
        }

        resWrapper.copyBodyToResponse();
    }

    private String truncate(String body, int maxLength) {
        if (body == null || body.isEmpty())
            return "<empty>";
        return body.length() > maxLength ? body.substring(0, maxLength) + "..." : body;
    }

}
