package ch.dboeckli.soap.service.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TraceParentFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    public TraceParentFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    // we set the traceparent header if it is not present in the request
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        filterChain.doFilter(request, response);

        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            String traceId = currentSpan.context().traceId();
            String spanId = currentSpan.context().spanId();
            response.setHeader("traceparent", "00-" + traceId + "-" + spanId + "-01");
        }
    }

}
