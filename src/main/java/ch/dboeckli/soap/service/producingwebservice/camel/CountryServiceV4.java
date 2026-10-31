package ch.dboeckli.soap.service.producingwebservice.camel;

import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryRequestV4;
import ch.dboeckli.soap.service.producingwebservice.schema.GetCountryResponseV4;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

@WebService(name = "CountriesPortV4", targetNamespace = "https://spring.io/guides/gs-producing-web-service")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL,
        parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface CountryServiceV4 {

    @WebMethod(operationName = "getCountry")
    @WebResult(name = "getCountryResponseV4", targetNamespace = "https://spring.io/guides/gs-producing-web-service",
            partName = "getCountryResponseV4")
    GetCountryResponseV4 getCountry(@WebParam(name = "getCountryRequestV4",
            targetNamespace = "https://spring.io/guides/gs-producing-web-service",
            partName = "getCountryRequestV4") GetCountryRequestV4 request);

}
