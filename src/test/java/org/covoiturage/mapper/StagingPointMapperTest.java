package org.covoiturage.mapper;

import org.covoiturage.dto.stagingPoint.StagingPointRequest;
import org.covoiturage.dto.stagingPoint.StagingPointResponse;
import org.covoiturage.dto.stagingPoint.StagingPointUpdateRequest;
import org.covoiturage.entity.Route;
import org.covoiturage.entity.StagingPoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StagingPointMapperTest {
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 12, 10, 18, 15);

    private final StagingPointMapper mapper = new StagingPointMapperImpl();

    @Test
    void toEntity_mapsFieldsAndIgnoresIdAndRoute() {
        StagingPointRequest request = StagingPointRequest.builder()
                .city("Casablanca")
                .stopOrder(1)
                .passingTime(TIME)
                .price(new BigDecimal("40.00"))
                .build();

        StagingPoint entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getRoute()).isNull();
        assertThat(entity.getCity()).isEqualTo("Casablanca");
        assertThat(entity.getStopOrder()).isEqualTo(1);
        assertThat(entity.getPassingTime()).isEqualTo(TIME);
        assertThat(entity.getPrice()).isEqualTo(new BigDecimal("40.00"));
    }

    @Test
    void toResponse_mapsAllFields() {
        StagingPointResponse response = mapper.toResponse(point());

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getCity()).isEqualTo("Casablanca");
        assertThat(response.getStopOrder()).isEqualTo(1);
        assertThat(response.getPassingTime()).isEqualTo(TIME);
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("40.00"));
    }

    @Test
    void toResponses_mapsEveryPoint() {
        List<StagingPointResponse> responses = mapper.toResponses(List.of(point(), point()));

        assertThat(responses).hasSize(2);
    }

    @Test
    void toRequest_mapsPointToRequest() {
        StagingPointRequest request = mapper.toRequest(point());

        assertThat(request.getCity()).isEqualTo("Casablanca");
        assertThat(request.getStopOrder()).isEqualTo(1);
        assertThat(request.getPassingTime()).isEqualTo(TIME);
        assertThat(request.getPrice()).isEqualTo(new BigDecimal("40.00"));
    }

    @Test
    void updateEntity_overwritesOnlyProvidedFields() {
        StagingPoint point = point();
        Route route = new Route();
        point.setRoute(route);
        StagingPointUpdateRequest request = new StagingPointUpdateRequest();
        request.setPrice(new BigDecimal("55.00"));

        mapper.updateEntity(request, point);

        assertThat(point.getPrice()).isEqualTo(new BigDecimal("55.00"));
        assertThat(point.getCity()).isEqualTo("Casablanca");
        assertThat(point.getPassingTime()).isEqualTo(TIME);
        assertThat(point.getId()).isEqualTo(2L);
        assertThat(point.getStopOrder()).isEqualTo(1);
        assertThat(point.getRoute()).isSameAs(route);
    }

    private StagingPoint point() {
        return StagingPoint.builder()
                .id(2L)
                .city("Casablanca")
                .stopOrder(1)
                .passingTime(TIME)
                .price(new BigDecimal("40.00"))
                .build();
    }
}
