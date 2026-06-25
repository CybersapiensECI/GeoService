package eci.edu.dosw.alpha.service;

import eci.edu.dosw.alpha.model.LocationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LocationServiceTest {

    private LocationService service;

    @BeforeEach
    void setUp() {
        service = new LocationService();
    }

    @Test
    void updateLocation_valid_storesAndReturns() {
        LocationMessage msg = new LocationMessage();
        msg.setUserId("u1");
        msg.setLat(4.6);
        msg.setLng(-74.0);

        LocationMessage result = service.updateLocation(msg);

        assertThat(result.getUserId()).isEqualTo("u1");
        assertThat(service.getLocation("u1")).isNotNull();
    }

    @Test
    void updateLocation_setsTimestampWhenZero() {
        LocationMessage msg = new LocationMessage();
        msg.setUserId("u2");
        msg.setLat(0);
        msg.setLng(0);
        msg.setTimestamp(0);

        service.updateLocation(msg);

        assertThat(msg.getTimestamp()).isGreaterThan(0);
    }

    @Test
    void updateLocation_keepsExistingTimestamp() {
        LocationMessage msg = new LocationMessage();
        msg.setUserId("u3");
        msg.setLat(10);
        msg.setLng(10);
        msg.setTimestamp(999L);

        service.updateLocation(msg);

        assertThat(msg.getTimestamp()).isEqualTo(999L);
    }

    @Test
    void updateLocation_nullUserId_throws() {
        LocationMessage msg = new LocationMessage();
        msg.setUserId(null);
        msg.setLat(0);
        msg.setLng(0);

        assertThatThrownBy(() -> service.updateLocation(msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void updateLocation_emptyUserId_throws() {
        LocationMessage msg = new LocationMessage();
        msg.setUserId("");
        msg.setLat(0);
        msg.setLng(0);

        assertThatThrownBy(() -> service.updateLocation(msg))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateLocation_invalidLatAbove90_throws() {
        LocationMessage msg = new LocationMessage();
        msg.setUserId("u4");
        msg.setLat(91);
        msg.setLng(0);

        assertThatThrownBy(() -> service.updateLocation(msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitud");
    }

    @Test
    void updateLocation_invalidLatBelow_throws() {
        LocationMessage msg = new LocationMessage();
        msg.setUserId("u5");
        msg.setLat(-91);
        msg.setLng(0);

        assertThatThrownBy(() -> service.updateLocation(msg))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateLocation_invalidLngAbove180_throws() {
        LocationMessage msg = new LocationMessage();
        msg.setUserId("u6");
        msg.setLat(0);
        msg.setLng(181);

        assertThatThrownBy(() -> service.updateLocation(msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Longitud");
    }

    @Test
    void updateLocation_invalidLngBelow_throws() {
        LocationMessage msg = new LocationMessage();
        msg.setUserId("u7");
        msg.setLat(0);
        msg.setLng(-181);

        assertThatThrownBy(() -> service.updateLocation(msg))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getLocation_nonExistentUser_returnsNull() {
        assertThat(service.getLocation("ghost")).isNull();
    }

    @Test
    void getAllLocations_afterTwoUpdates_returnsBoth() {
        LocationMessage m1 = new LocationMessage();
        m1.setUserId("a"); m1.setLat(1); m1.setLng(1);
        LocationMessage m2 = new LocationMessage();
        m2.setUserId("b"); m2.setLat(2); m2.setLng(2);

        service.updateLocation(m1);
        service.updateLocation(m2);

        assertThat(service.getAllLocations()).hasSize(2);
    }

    @Test
    void updateLocation_boundaryLat90_valid() {
        LocationMessage msg = new LocationMessage();
        msg.setUserId("bound1");
        msg.setLat(90);
        msg.setLng(0);
        assertThatNoException().isThrownBy(() -> service.updateLocation(msg));
    }

    @Test
    void updateLocation_boundaryLng180_valid() {
        LocationMessage msg = new LocationMessage();
        msg.setUserId("bound2");
        msg.setLat(0);
        msg.setLng(180);
        assertThatNoException().isThrownBy(() -> service.updateLocation(msg));
    }
}
