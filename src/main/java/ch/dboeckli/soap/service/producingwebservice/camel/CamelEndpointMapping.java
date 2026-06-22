package ch.dboeckli.soap.service.producingwebservice.camel;

import org.springframework.context.annotation.Bean;

public class CamelEndpointMapping {

    @Bean
    public CamelEndpointMapping endpointMapping() {
        return new CamelEndpointMapping();
    }

}
