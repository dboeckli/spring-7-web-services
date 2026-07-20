package ch.dboeckli.soap.service.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TracingHttpExchangeRepository implements HttpExchangeRepository {

    private final Tracer tracer;

    private final InMemoryHttpExchangeRepository delegate;

    public TracingHttpExchangeRepository(Tracer tracer) {
        this.tracer = tracer;
        this.delegate = new InMemoryHttpExchangeRepository();
    }

    @Override
    public List<HttpExchange> findAll() {
        return delegate.findAll();
    }

    @Override
    public void add(@NonNull HttpExchange exchange) {
        delegate.add(exchange); // ← weiterhin im Memory speichern

        // ← zusätzlich als Span/Trace speichern
        assert exchange.getTimeTaken() != null;
        Span span = tracer.nextSpan()
            .name("http.exchange")
            .tag("http.method", exchange.getRequest().getMethod())
            .tag("http.uri", exchange.getRequest().getUri().toString())
            .tag("http.status", String.valueOf(exchange.getResponse().getStatus()))
            .tag("time_taken", exchange.getTimeTaken().toString())
            .start();

        // Headers als Tags
        exchange.getRequest()
            .getHeaders()
            .forEach((key, values) -> span.tag("request.header." + key, String.join(",", values)));

        span.end();
    }

}
