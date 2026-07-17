package eci.edu.dosw.alpha.model;

/**
 * Geocerca real de una zona del campus: centro (lat/lng), radio en metros,
 * y el {@code activityType} de GamificationService que dispara al entrar.
 * Coordenadas verificadas en campo/Google Maps (ver MONAS_GEOLOCALIZACION.md).
 */
public record GeoFenceZone(
        CampusZone zone,
        double lat,
        double lng,
        double radiusMeters,
        String activityType
) {
    public String locationCode() {
        return zone.name();
    }
}
