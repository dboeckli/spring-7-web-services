package ch.dboeckli.soap.service.producingwebservice.rest;

import ch.dboeckli.soap.service.producingwebservice.CountryRepository;
import ch.dboeckli.soap.service.producingwebservice.schema.Country;
import io.opentelemetry.api.baggage.Baggage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/countries")
@Slf4j
public class CountryRestController {

    private final CountryRepository countryRepository;

    public CountryRestController(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @GetMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Country> getCountry(@PathVariable String name) {
        String baggageValue = Baggage.current().getEntryValue("testBaggage");
        log.info("Empfangener Baggage-Wert 'testBaggage': {}", baggageValue);

        Country country = countryRepository.findCountry(name);
        if (country == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(country);
    }

}
