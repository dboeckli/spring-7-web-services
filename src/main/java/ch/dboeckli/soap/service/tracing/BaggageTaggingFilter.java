package ch.dboeckli.soap.service.tracing;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class BaggageTaggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Baggage updatedBaggage = Baggage.current().toBuilder().put("addedBaggage", "echo").build();

        try (Scope ignored = updatedBaggage.makeCurrent()) {
            // 3. Optional: Alle Baggage-Felder (inkl. des neuen) als Span-Attribute
            // setzen
            // Damit sie in Elastic APM als Labels erscheinen
            updatedBaggage.asMap().forEach((key, entry) -> Span.current().setAttribute(key, entry.getValue()));
            // 4. Den Request weiterlaufen lassen (innerhalb des Scopes!)
            filterChain.doFilter(request, response);
        }

    }

}
