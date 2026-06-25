package eci.edu.dosw.alpha.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private static String buildJwt(String sub) {
        String header  = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + sub + "\",\"iat\":1700000000}").getBytes());
        return "Bearer " + header + "." + payload + ".fakesig";
    }

    @Test
    void extractUserId_validJwt_returnsSub() {
        String token = buildJwt("user123");
        assertThat(JwtUtil.extractUserId(token)).isEqualTo("user123");
    }

    @Test
    void extractUserId_nullHeader_throws() {
        assertThatThrownBy(() -> JwtUtil.extractUserId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authorization");
    }

    @Test
    void extractUserId_missingBearerPrefix_throws() {
        assertThatThrownBy(() -> JwtUtil.extractUserId("Token abc.def.ghi"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authorization");
    }

    @Test
    void extractUserId_twoPartToken_throws() {
        assertThatThrownBy(() -> JwtUtil.extractUserId("Bearer header.payload"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT");
    }

    @Test
    void extractUserId_payloadWithoutSub_throws() {
        String header  = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"iat\":1700000000}".getBytes());
        String token = "Bearer " + header + "." + payload + ".sig";

        assertThatThrownBy(() -> JwtUtil.extractUserId(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sub");
    }

    @Test
    void extractUserId_blankSub_throws() {
        String token = buildJwt("   ");
        assertThatThrownBy(() -> JwtUtil.extractUserId(token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractUserId_payloadWithPadding_ok() {
        // userId de longitud que fuerza padding en base64
        String token = buildJwt("ab");
        assertThat(JwtUtil.extractUserId(token)).isEqualTo("ab");
    }

    @Test
    void extractUserId_emptyBearerValue_throws() {
        assertThatThrownBy(() -> JwtUtil.extractUserId("Bearer "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
