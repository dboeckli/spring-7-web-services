package ch.dboeckli.soap.service.producingwebservice;

import ch.dboeckli.soap.service.producingwebservice.schema.Country;
import ch.dboeckli.soap.service.producingwebservice.schema.Currency;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class CountryRepositoryTest {

    private final CountryRepository repository = new CountryRepository();

    @Test
    void findCountryReturnsExpectedCountry() {
        Country result = repository.findCountry("Spain");

        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result.getName()).isEqualTo("Spain"),
                () -> assertThat(result.getCapital()).isEqualTo("Madrid"),
                () -> assertThat(result.getCurrency()).isEqualTo(Currency.EUR),
                () -> assertThat(result.getPopulation()).isEqualTo(46_704_314));
    }

    @Test
    void findCountryReturnsNullForUnknownCountry() {
        Country result = repository.findCountry("Unknown");

        assertThat(result).isNull();
    }

    @Test
    void findCountryRejectsNullName() {
        assertThatThrownBy(() -> repository.findCountry(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be null");
    }

}
