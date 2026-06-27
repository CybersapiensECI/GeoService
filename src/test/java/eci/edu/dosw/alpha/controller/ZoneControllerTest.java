package eci.edu.dosw.alpha.controller;

import eci.edu.dosw.alpha.dto.ZoneRequest;
import eci.edu.dosw.alpha.dto.ZoneResponse;
import eci.edu.dosw.alpha.service.ZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoneControllerTest {

    @Mock ZoneService zoneService;
    @InjectMocks ZoneController controller;

    private static String buildJwt(String sub) {
        String header  = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + sub + "\",\"iat\":1700000000}").getBytes());
        return "Bearer " + header + "." + payload + ".fakesig";
    }

    private String authHeader;
    private ZoneRequest request;
    private ZoneResponse response;

    @BeforeEach
    void setUp() {
        authHeader = buildJwt("user1");
        request = new ZoneRequest();
        request.setCampusZone("BIBLIOTECA");
        request.setGeoLocationEnabled(true);
        response = new ZoneResponse("BIBLIOTECA", List.of("BLOQUE_A", "ENTRADA_PRINCIPAL"));
    }

    @Test
    void saveZone_validJwt_delegatesToService() {
        when(zoneService.saveZone("user1", request)).thenReturn(response);

        ResponseEntity<ZoneResponse> res = controller.saveZone(authHeader, request);

        assertThat(res.getBody().getCurrentZone()).isEqualTo("BIBLIOTECA");
        verify(zoneService).saveZone("user1", request);
    }

    @Test
    void saveZone_invalidJwt_throws() {
        assertThatThrownBy(() -> controller.saveZone("BadHeader", request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getMyZone_found_returnsOk() {
        when(zoneService.getZone("user1")).thenReturn(response);

        ResponseEntity<ZoneResponse> res = controller.getMyZone(authHeader);

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().getCurrentZone()).isEqualTo("BIBLIOTECA");
    }

    @Test
    void getMyZone_notFound_returns404() {
        when(zoneService.getZone("user1")).thenReturn(null);

        ResponseEntity<ZoneResponse> res = controller.getMyZone(authHeader);

        assertThat(res.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void getCatalog_returnsAllZones() {
        ResponseEntity<List<String>> res = controller.getCatalog();

        assertThat(res.getBody()).contains("BIBLIOTECA", "BLOQUE_A", "CAFETERIA");
        assertThat(res.getStatusCode().value()).isEqualTo(200);
    }
}
