package eci.edu.dosw.alpha.service;

import eci.edu.dosw.alpha.model.CampusZone;
import eci.edu.dosw.alpha.model.GeoFenceZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeofenceServiceTest {

    private GeofenceService service;

    @BeforeEach
    void setUp() {
        service = new GeofenceService(new GeofenceCatalog());
    }

    @Test
    void resolveZone_exactCenterOfEdificioA_matchesEdificioA() {
        var result = service.resolveZone(4.782699855684707, -74.04264918379);

        assertThat(result).isPresent();
        assertThat(result.get().zone()).isEqualTo(CampusZone.EDIFICIO_A);
        assertThat(result.get().activityType()).isEqualTo("BUILDING_CHECKIN");
    }

    @Test
    void resolveZone_exactCenterOfLago_matchesLagoWithCampusZoneCheckin() {
        var result = service.resolveZone(4.783042637971193, -74.04428261122634);

        assertThat(result).isPresent();
        assertThat(result.get().zone()).isEqualTo(CampusZone.LAGO);
        assertThat(result.get().activityType()).isEqualTo("CAMPUS_ZONE_CHECKIN");
    }

    @Test
    void resolveZone_exactCenterOfCafeteriaRegio_matchesCafeteriaCheckin() {
        var result = service.resolveZone(4.782980848558539, -74.04400996864427);

        assertThat(result).isPresent();
        assertThat(result.get().zone()).isEqualTo(CampusZone.CAFETERIA_REGIO);
        assertThat(result.get().activityType()).isEqualTo("CAFETERIA_CHECKIN");
    }

    @Test
    void resolveZone_farFromCampus_returnsEmpty() {
        var result = service.resolveZone(0.0, 0.0);

        assertThat(result).isEmpty();
    }

    @Test
    void distanceMeters_sameCoordinate_isZero() {
        assertThat(service.distanceMeters(4.78, -74.04, 4.78, -74.04)).isZero();
    }

    @Test
    void distanceMeters_knownOneDegreeLatitude_isApprox111km() {
        double distance = service.distanceMeters(0.0, 0.0, 1.0, 0.0);

        assertThat(distance).isBetween(110_000.0, 112_000.0);
    }
}
