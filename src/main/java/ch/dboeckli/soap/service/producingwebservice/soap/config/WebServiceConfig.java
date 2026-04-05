package ch.dboeckli.soap.service.producingwebservice.soap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.config.annotation.WsConfigurer;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class WebServiceConfig implements WsConfigurer {

    @Bean
    public DefaultWsdl11Definition countries(SimpleXsdSchema countriesWs) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("CountriesPort");
        wsdl11Definition.setLocationUri("/services");
        wsdl11Definition.setTargetNamespace("https://spring.io/guides/gs-producing-web-service");
        wsdl11Definition.setSchema(countriesWs);
        return wsdl11Definition;
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(new CustomEndpointInterceptor());
    }

}
