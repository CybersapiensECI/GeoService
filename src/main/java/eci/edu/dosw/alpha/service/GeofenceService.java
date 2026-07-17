package eci.edu.dosw.alpha.service;

import eci.edu.dosw.alpha.model.GeoFenceZone;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;

/**
 * Resuelve en qué geocerca real del campus cae una posición GPS.
 * Distancia por fórmula de Haversine (radio terrestre medio 6 371 000 m).
 */
@Service
public class GeofenceService {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final GeofenceCatalog catalog;

    public GeofenceService(GeofenceCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Zona que contiene el punto dado, si existe. Si el punto cae dentro de
     * varias geocercas superpuestas, retorna la más cercana al centro.
     */
    public Optional<GeoFenceZone> resolveZone(double lat, double lng) {
        return catalog.getZones().stream()
                .filter(z -> distanceMeters(lat, lng, z.lat(), z.lng()) <= z.radiusMeters())
                .min(Comparator.comparingDouble(z -> distanceMeters(lat, lng, z.lat(), z.lng())));
    }

    public double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
