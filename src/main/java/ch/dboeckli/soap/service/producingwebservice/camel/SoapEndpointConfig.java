package ch.dboeckli.soap.service.producingwebservice.camel;

import org.apache.camel.component.spring.ws.bean.CamelEndpointMapping;
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

}
