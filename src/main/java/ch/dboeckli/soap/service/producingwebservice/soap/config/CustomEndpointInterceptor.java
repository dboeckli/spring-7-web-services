package ch.dboeckli.soap.service.producingwebservice.soap.config;

import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.server.endpoint.MethodEndpoint;

@Slf4j
public class CustomEndpointInterceptor implements EndpointInterceptor {

    @Override
    public boolean handleRequest(@NonNull MessageContext messageContext, @NonNull Object endpoint) {
        log.debug("Endpoint Request Handling, messageContext");
        if (endpoint instanceof MethodEndpoint methodEndpoint) {
            Span currentSpan = Span.current();
            if (currentSpan != null && currentSpan.isRecording()) {
                currentSpan.setAttribute("soap.method", methodEndpoint.getMethod().getName());
                currentSpan.setAttribute("soap.endpoint", endpoint.getClass().getSimpleName());
            }
        }
        return true;
    }

    @Override
    public boolean handleResponse(@NonNull MessageContext messageContext, @NonNull Object endpoint) {
        log.debug("Endpoint Response Handling");
        return true;
    }

    @Override
    public boolean handleFault(@NonNull MessageContext messageContext, @NonNull Object endpoint) {
        log.debug("Endpoint Exception Handling");
        return true;
    }

    @Override
    public void afterCompletion(@NonNull MessageContext messageContext, @NonNull Object endpoint, Exception ex) {
        log.debug("Execute code after completion");
    }

}
