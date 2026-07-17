package eci.edu.dosw.alpha.service;

import eci.edu.dosw.alpha.dto.ZoneRequest;
import eci.edu.dosw.alpha.dto.ZoneResponse;
import eci.edu.dosw.alpha.exception.InvalidZoneException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ZoneServiceTest {

    private ZoneService service;

    @BeforeEach
    void setUp() {
        service = new ZoneService();
    }

    // ── saveZone ──────────────────────────────────────────────────────────────

    @Test
    void saveZone_validZone_returnsCurrentZoneAndNearby() {
        ZoneRequest req = new ZoneRequest();
        req.setCampusZone("BIBLIOTECA");
        req.setGeoLocationEnabled(true);

        ZoneResponse res = service.saveZone("user1", req);

        assertThat(res.getCurrentZone()).isEqualTo("BIBLIOTECA");
        assertThat(res.getNearbyParches()).isNotEmpty();
    }

    @Test
    void saveZone_geoDisabled_returnsNullZoneAndEmptyNearby() {
        ZoneRequest req = new ZoneRequest();
        req.setCampusZone("CAFETERIA_REGIO");
        req.setGeoLocationEnabled(false);

        ZoneResponse res = service.saveZone("user2", req);

        assertThat(res.getCurrentZone()).isNull();
        assertThat(res.getNearbyParches()).isEmpty();
    }

    @Test
    void saveZone_invalidZone_throwsInvalidZoneException() {
        ZoneRequest req = new ZoneRequest();
        req.setCampusZone("ZONA_INEXISTENTE");
        req.setGeoLocationEnabled(true);

        assertThatThrownBy(() -> service.saveZone("user3", req))
                .isInstanceOf(InvalidZoneException.class)
                .hasMessageContaining("ZONA_INEXISTENTE");
    }

    @Test
    void saveZone_blankZone_throwsInvalidZoneException() {
        ZoneRequest req = new ZoneRequest();
        req.setCampusZone("   ");
        req.setGeoLocationEnabled(true);

        assertThatThrownBy(() -> service.saveZone("user4", req))
                .isInstanceOf(InvalidZoneException.class);
    }

    @Test
    void saveZone_nullZone_throwsInvalidZoneException() {
        ZoneRequest req = new ZoneRequest();
        req.setCampusZone(null);
        req.setGeoLocationEnabled(true);

        assertThatThrownBy(() -> service.saveZone("user5", req))
                .isInstanceOf(InvalidZoneException.class);
    }

    @Test
    void saveZone_caseInsensitive_accepted() {
        ZoneRequest req = new ZoneRequest();
        req.setCampusZone("cancha");
        req.setGeoLocationEnabled(true);

        ZoneResponse res = service.saveZone("user6", req);

        assertThat(res.getCurrentZone()).isEqualTo("CANCHA");
    }

    // ── getZone ──────────────────────────────────────────────────────────────

    @Test
    void getZone_unknownUser_returnsNull() {
        ZoneResponse res = service.getZone("nobody");
        assertThat(res).isNull();
    }

    @Test
    void getZone_savedAndEnabled_returnsZone() {
        ZoneRequest req = new ZoneRequest();
        req.setCampusZone("AUDITORIO");
        req.setGeoLocationEnabled(true);
        service.saveZone("userA", req);

        ZoneResponse res = service.getZone("userA");

        assertThat(res.getCurrentZone()).isEqualTo("AUDITORIO");
        assertThat(res.getNearbyParches()).contains("CANCHA", "ENTRADA_PRINCIPAL");
    }

    @Test
    void getZone_savedButDisabled_E2_returnsNullZone() {
        ZoneRequest req = new ZoneRequest();
        req.setCampusZone("EDIFICIO_A");
        req.setGeoLocationEnabled(false);
        service.saveZone("userB", req);

        ZoneResponse res = service.getZone("userB");

        assertThat(res.getCurrentZone()).isNull();
        assertThat(res.getNearbyParches()).isEmpty();
    }

    @Test
    void saveZone_allValidZones_doNotThrow() {
        String[] zones = {
            "EDIFICIO_A","EDIFICIO_B","EDIFICIO_C","EDIFICIO_D","EDIFICIO_E",
            "EDIFICIO_F","EDIFICIO_G","EDIFICIO_H","EDIFICIO_I",
            "CAFETERIA_REGIO","CAFETERIA_2","CAFETERIA_3","CAFETERIA_4",
            "LAGO","REFLEXION",
            "BIBLIOTECA","CANCHA","AUDITORIO","ENTRADA_PRINCIPAL","PARQUEADERO"
        };
        for (String z : zones) {
            ZoneRequest req = new ZoneRequest();
            req.setCampusZone(z);
            req.setGeoLocationEnabled(true);
            assertThatNoException().isThrownBy(() -> service.saveZone("u", req));
        }
    }

    @Test
    void saveZone_overwritesPreviousPreference() {
        ZoneRequest first = new ZoneRequest();
        first.setCampusZone("EDIFICIO_A");
        first.setGeoLocationEnabled(true);
        service.saveZone("userC", first);

        ZoneRequest second = new ZoneRequest();
        second.setCampusZone("PARQUEADERO");
        second.setGeoLocationEnabled(true);
        service.saveZone("userC", second);

        assertThat(service.getZone("userC").getCurrentZone()).isEqualTo("PARQUEADERO");
    }

    @Test
    void nearbyParches_bloqueA_containsExpected() {
        ZoneRequest req = new ZoneRequest();
        req.setCampusZone("EDIFICIO_A");
        req.setGeoLocationEnabled(true);

        ZoneResponse res = service.saveZone("u7", req);

        assertThat(res.getNearbyParches()).containsExactlyInAnyOrder("EDIFICIO_B", "CAFETERIA_REGIO");
    }
}
