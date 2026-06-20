package eci.edu.dosw.alpha.util;

import java.util.Base64;

public class JwtUtil {

    private JwtUtil() {}

    /**
     * Extrae el userId (claim "sub") del Bearer token sin verificar firma.
     * Usa solo java.util.Base64 — sin dependencias externas.
     */
    public static String extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Se requiere Authorization: Bearer <token>");
        }
        try {
            String token = authHeader.substring(7).trim();
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Formato JWT inválido");
            }
            String paddedPayload = parts[1];
            int mod = paddedPayload.length() % 4;
            if (mod != 0) paddedPayload += "=".repeat(4 - mod);

            String json = new String(Base64.getUrlDecoder().decode(paddedPayload));
            String sub = extractClaim(json, "sub");
            if (sub == null || sub.isBlank()) {
                throw new IllegalArgumentException("JWT no contiene claim 'sub'");
            }
            return sub;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("JWT inválido: " + e.getMessage());
        }
    }

    /** Extrae el valor string de un claim del JSON del payload JWT. */
    private static String extractClaim(String json, String claim) {
        String key = "\"" + claim + "\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(":", keyIdx + key.length());
        if (colonIdx < 0) return null;
        int valueStart = colonIdx + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') return null;
        valueStart++;
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd < 0) return null;
        return json.substring(valueStart, valueEnd);
    }
}
