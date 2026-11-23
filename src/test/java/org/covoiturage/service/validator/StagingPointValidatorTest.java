package org.covoiturage.service.validator;

import org.covoiturage.dto.stagingPoint.StagingPointRequest;
import org.covoiturage.testutil.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.covoiturage.testutil.ApiAssert.assertFails;

class StagingPointValidatorTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 22, 12, 0);

    private final StagingPointValidator validator = new StagingPointValidator();
    private List<StagingPointRequest> points;

    @BeforeEach
    void setUp() {
        points = TestData.pointRequests(NOW.plusDays(1));
    }

    @Test
    void validate_acceptsValidPoints() {
        List<StagingPointRequest> result = validator.validateAndSort(points, NOW);

        assertThat(result).extracting(StagingPointRequest::getCity)
                .containsExactly("Rabat", "Casablanca", "Settat", "Marrakech");
    }

    @Test
    void validate_sortsPointsByStopOrder() {
        Collections.reverse(points);

        List<StagingPointRequest> result = validator.validateAndSort(points, NOW);

        assertThat(result).extracting(StagingPointRequest::getStopOrder).containsExactly(0, 1, 2, 3);
    }

    @Test
    void validate_refusesNullList() {
        assertFails(() -> validator.validateAndSort(null, NOW), HttpStatus.BAD_REQUEST, "au moins deux points");
    }

    @Test
    void validate_refusesSinglePoint() {
        assertFails(() -> validator.validateAndSort(points.subList(0, 1), NOW),
                HttpStatus.BAD_REQUEST, "au moins deux points");
    }

    @ParameterizedTest
    @CsvSource({"0,2", "1,2", "0,0"})
    void validate_refusesInvalidOrders(int first, int second) {
        List<StagingPointRequest> two = List.of(
                request("Rabat", first, NOW.plusDays(1), "0.00"),
                request("Casablanca", second, NOW.plusDays(1).plusHours(1), "40.00"));

        assertFails(() -> validator.validateAndSort(two, NOW), HttpStatus.BAD_REQUEST, "ordres");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -60})
    void validate_refusesDepartureNotInFuture(long minutesFromNow) {
        points.get(0).setPassingTime(NOW.plusMinutes(minutesFromNow));

        assertFails(() -> validator.validateAndSort(points, NOW), HttpStatus.BAD_REQUEST, "dans le futur");
    }

    @Test
    void validate_refusesNonZeroDeparturePrice() {
        points.get(0).setPrice(new BigDecimal("5.00"));

        assertFails(() -> validator.validateAndSort(points, NOW), HttpStatus.BAD_REQUEST, "égal à 0");
    }

    @Test
    void validate_refusesRepeatedCityIgnoringCase() {
        points.get(2).setCity("RABAT");

        assertFails(() -> validator.validateAndSort(points, NOW), HttpStatus.BAD_REQUEST, "apparaît plusieurs fois");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -10})
    void validate_refusesTimeNotStrictlyIncreasing(long minutesAfterPrevious) {
        points.get(2).setPassingTime(points.get(1).getPassingTime().plusMinutes(minutesAfterPrevious));

        assertFails(() -> validator.validateAndSort(points, NOW), HttpStatus.BAD_REQUEST, "strictement postérieure");
    }

    @ParameterizedTest
    @ValueSource(strings = {"40.00", "10.00"})
    void validate_refusesPriceNotStrictlyIncreasing(String price) {
        points.get(2).setPrice(new BigDecimal(price));

        assertFails(() -> validator.validateAndSort(points, NOW), HttpStatus.BAD_REQUEST, "strictement supérieur");
    }

    private StagingPointRequest request(String city, int order, LocalDateTime time, String price) {
        return StagingPointRequest.builder()
                .city(city)
                .stopOrder(order)
                .passingTime(time)
                .price(new BigDecimal(price))
                .build();
    }
}
