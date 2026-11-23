package org.covoiturage.mapper;

import org.covoiturage.dto.route.RouteResponse;
import org.covoiturage.dto.stagingPoint.StagingPointResponse;
import org.covoiturage.entity.Route;
import org.covoiturage.enums.RouteStatus;
import org.covoiturage.testutil.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {RouteMapperImpl.class, StagingPointMapperImpl.class})
class RouteMapperTest {

    @Autowired
    private RouteMapper mapper;

    @Test
    void toResponse_mapsRouteVehicleIdAndOrderedPoints() {
        LocalDateTime start = LocalDateTime.of(2026, 12, 10, 17, 0);
        Route route = TestData.route(5L, TestData.vehicle(9L, TestData.driver(UUID.randomUUID()), 4),
                3, RouteStatus.PUBLISHED, start);

        RouteResponse response = mapper.toResponse(route);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getStartCity()).isEqualTo("Rabat");
        assertThat(response.getArrivalCity()).isEqualTo("Marrakech");
        assertThat(response.getStartDate()).isEqualTo(start);
        assertThat(response.getAvailablePlaces()).isEqualTo(3);
        assertThat(response.getStatus()).isEqualTo(RouteStatus.PUBLISHED);
        assertThat(response.getVehicleId()).isEqualTo(9L);
        assertThat(response.getStagingPoints()).extracting(StagingPointResponse::getCity)
                .containsExactly("Rabat", "Casablanca", "Settat", "Marrakech");
    }

    @Test
    void toResponses_mapsEveryRoute() {
        Route first = TestData.route(1L, TestData.vehicle(1L, TestData.driver(UUID.randomUUID()), 4),
                3, RouteStatus.PUBLISHED, LocalDateTime.now().plusDays(1));
        Route second = TestData.route(2L, TestData.vehicle(1L, TestData.driver(UUID.randomUUID()), 4),
                2, RouteStatus.FULL, LocalDateTime.now().plusDays(2));

        List<RouteResponse> responses = mapper.toResponses(List.of(first, second));

        assertThat(responses).extracting(RouteResponse::getId).containsExactly(1L, 2L);
        assertThat(responses).extracting(RouteResponse::getStatus)
                .containsExactly(RouteStatus.PUBLISHED, RouteStatus.FULL);
    }
}
