package eci.edu.dosw.alpha.exception;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleInvalidZone_returnsE1Code() {
        InvalidZoneException ex = new InvalidZoneException("ZONA_X");
        Map<String, String> body = handler.handleInvalidZone(ex);

        assertThat(body.get("code")).isEqualTo("E1");
        assertThat(body.get("error")).contains("ZONA_X");
    }

    @Test
    void handleIllegalArgument_returnsErrorMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("bad input");
        Map<String, String> body = handler.handleIllegalArgument(ex);

        assertThat(body.get("error")).isEqualTo("bad input");
    }

    @Test
    void invalidZoneException_messageContainsZoneName() {
        InvalidZoneException ex = new InvalidZoneException("ZONA_FALSA");
        assertThat(ex.getMessage()).contains("ZONA_FALSA");
    }
}
