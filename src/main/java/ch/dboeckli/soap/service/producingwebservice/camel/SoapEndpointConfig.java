package ch.dboeckli.soap.service.producingwebservice.camel;

import org.apache.camel.component.spring.ws.bean.CamelEndpointMapping;
import org.apache.cxf.spring.boot.autoconfigure.CxfProperties;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.server.endpoint.adapter.MessageEndpointAdapter;

@Configuration
public class SoapEndpointConfig {

    @Bean
    public CamelEndpointMapping endpointMapping() {
        return new CamelEndpointMapping();
    }

    @Bean
    public MessageEndpointAdapter messageEndpointAdapter() {
        return new MessageEndpointAdapter();
    }

    /**
     * Registers the CXFServlet for the V4 endpoint. Async/continuation dispatch is
     * disabled on purpose: the {@code PayloadLoggingFilter} wraps responses with a
     * {@code ContentCachingResponseWrapper}, which would lose the body written after the
     * async dispatch completed.
     */
    @Bean
    public ServletRegistrationBean<CXFServlet> cxfServletRegistration(CxfProperties properties) {
        String path = properties.getPath();
        String urlMapping = path.endsWith("/") ? path + "*" : path + "/*";
        ServletRegistrationBean<CXFServlet> registration = new ServletRegistrationBean<>(new CXFServlet(), urlMapping);
        registration.setLoadOnStartup(properties.getServlet().getLoadOnStartup());
        properties.getServlet().getInit().forEach(registration::addInitParameter);
        registration.setAsyncSupported(false);
        return registration;
    }

}
