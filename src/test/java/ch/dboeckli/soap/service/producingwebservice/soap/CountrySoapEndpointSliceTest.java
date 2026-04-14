package ch.dboeckli.soap.service.producingwebservice.soap;

import ch.dboeckli.soap.service.producingwebservice.CountryRepository;
import ch.dboeckli.soap.service.producingwebservice.schema.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webservices.test.autoconfigure.server.WebServiceServerTest;
import org.springframework.context.annotation.Bean;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.ClassUtils;
import org.springframework.ws.test.server.MockWebServiceClient;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.noFault;

@WebServiceServerTest(CountrySoapEndpoint.class)
class CountrySoapEndpointSliceTest {

    @TestConfiguration
    static class MarshallerConfig {

        @Bean
        Jaxb2Marshaller jaxb2Marshaller() {
            Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
            marshaller.setPackagesToScan(ClassUtils.getPackageName(GetCountryRequest.class));
            return marshaller;
        }

    }

    @Autowired
    private MockWebServiceClient client;

    @Autowired
    private Jaxb2Marshaller jaxb2Marshaller;

    @MockitoBean
    private CountryRepository countryRepository;

    @Test
    void getCountryReturnsExpectedResponse() {
        Country spain = new Country();
        spain.setName("Spain");
        spain.setCapital("Madrid");
        spain.setCurrency(Currency.EUR);
        spain.setPopulation(46_704_314);
        when(countryRepository.findCountry("Spain")).thenReturn(spain);

        GetCountryRequest request = new GetCountryRequest();
        request.setName("Spain");

        StringWriter sw = new StringWriter();
        jaxb2Marshaller.marshal(request, new StreamResult(sw));
        Source requestPayload = new StreamSource(new StringReader(sw.toString()));

        GetCountryResponse[] holder = new GetCountryResponse[1];
        client.sendRequest(withPayload(requestPayload))
            .andExpect(noFault())
            .andExpect(
                    (req, res) -> holder[0] = (GetCountryResponse) jaxb2Marshaller.unmarshal(res.getPayloadSource()));

        assertThat(holder[0].getCountry().getCapital()).isEqualTo("Madrid");
        assertThat(holder[0].getCountry().getCurrency()).isEqualTo(Currency.EUR);
    }

}