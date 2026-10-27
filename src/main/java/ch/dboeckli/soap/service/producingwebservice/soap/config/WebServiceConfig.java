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
        return wsdlDefinition(countriesWs, "CountriesPort", "Request", "Response");
    }

    @Bean
    public DefaultWsdl11Definition countriesV2(SimpleXsdSchema countriesWs) {
        return wsdlDefinition(countriesWs, "CountriesPortV2", "RequestV2", "ResponseV2");
    }

    @Bean
    public DefaultWsdl11Definition countriesV3(SimpleXsdSchema countriesWs) {
        return wsdlDefinition(countriesWs, "CountriesPortV3", "RequestV3", "ResponseV3");
    }

    private DefaultWsdl11Definition wsdlDefinition(SimpleXsdSchema countriesWs, String portTypeName,
            String requestSuffix, String responseSuffix) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName(portTypeName);
        wsdl11Definition.setLocationUri("/services");
        wsdl11Definition.setTargetNamespace("https://spring.io/guides/gs-producing-web-service");
        wsdl11Definition.setSchema(countriesWs);
        wsdl11Definition.setRequestSuffix(requestSuffix);
        wsdl11Definition.setResponseSuffix(responseSuffix);
        return wsdl11Definition;
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(new CustomEndpointInterceptor());
    }

}
