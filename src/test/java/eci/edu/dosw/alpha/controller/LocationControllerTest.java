package eci.edu.dosw.alpha.controller;

import eci.edu.dosw.alpha.model.LocationMessage;
import eci.edu.dosw.alpha.service.LocationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    @Mock LocationService locationService;
    @InjectMocks LocationController controller;

    @Test
    void receiveLocation_delegatesToService() {
        LocationMessage msg = new LocationMessage();
        msg.setUserId("u1");
        msg.setLat(4.6);
        msg.setLng(-74.0);
        when(locationService.updateLocation(msg)).thenReturn(msg);

        LocationMessage result = controller.receiveLocation(msg);

        assertThat(result.getUserId()).isEqualTo("u1");
        verify(locationService).updateLocation(msg);
    }

    @Test
    void getLocation_existingUser_returnsMessage() {
        LocationMessage msg = new LocationMessage();
        msg.setUserId("u2");
        when(locationService.getLocation("u2")).thenReturn(msg);

        LocationMessage result = controller.getLocation("u2");

        assertThat(result.getUserId()).isEqualTo("u2");
    }

    @Test
    void getLocation_notFound_returnsNull() {
        when(locationService.getLocation("ghost")).thenReturn(null);

        assertThat(controller.getLocation("ghost")).isNull();
    }
}
