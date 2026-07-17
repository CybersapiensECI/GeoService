package eci.edu.dosw.alpha.service;

import eci.edu.dosw.alpha.model.GeoFenceZone;
import eci.edu.dosw.alpha.model.LocationMessage;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocationService {

    // Última ubicación por usuario
    private final ConcurrentHashMap<String, LocationMessage> locations = new ConcurrentHashMap<>();

    // Última geocerca (locationCode) en la que se detectó a cada usuario;
    // evita re-notificar a Gamification en cada ping mientras sigue dentro.
    private final ConcurrentHashMap<String, String> lastZoneByUser = new ConcurrentHashMap<>();

    private final GeofenceService geofenceService;
    private final GamificationActivityClient gamificationActivityClient;

    public LocationService(GeofenceService geofenceService, GamificationActivityClient gamificationActivityClient) {
        this.geofenceService = geofenceService;
        this.gamificationActivityClient = gamificationActivityClient;
    }

    /**
     * Actualiza la ubicación de un usuario
     */
    public LocationMessage updateLocation(LocationMessage message) {
        validate(message);

        // agregar timestamp si no viene
        if (message.getTimestamp() == 0) {
            message.setTimestamp(System.currentTimeMillis());
        }

        locations.put(message.getUserId(), message);
        checkGeofenceEntry(message);
        return message;
    }

    /**
     * Detecta si el usuario entró a una geocerca nueva (transición
     * fuera→dentro o cambio de zona) y, si es así, notifica a
     * GamificationService de forma asíncrona.
     */
    private void checkGeofenceEntry(LocationMessage message) {
        String userId = message.getUserId();
        Optional<GeoFenceZone> current = geofenceService.resolveZone(message.getLat(), message.getLng());

        if (current.isEmpty()) {
            lastZoneByUser.remove(userId);
            return;
        }

        GeoFenceZone zone = current.get();
        String previousZone = lastZoneByUser.put(userId, zone.locationCode());
        if (!zone.locationCode().equals(previousZone)) {
            gamificationActivityClient.notifyCheckin(userId, zone.activityType(), zone.locationCode());
        }
    }

    /**
     * Obtener ubicación de un usuario
     */
    public LocationMessage getLocation(String userId) {
        return locations.get(userId);
    }

    /**
     * Obtener todas las ubicaciones activas
     */
    public Collection<LocationMessage> getAllLocations() {
        return locations.values();
    }

    /**
     * Validaciones básicas (MUY importante)
     */
    private void validate(LocationMessage message) {

        if (message.getUserId() == null || message.getUserId().isEmpty()) {
            throw new IllegalArgumentException("userId es obligatorio");
        }

        if (message.getLat() < -90 || message.getLat() > 90) {
            throw new IllegalArgumentException("Latitud inválida");
        }

        if (message.getLng() < -180 || message.getLng() > 180) {
            throw new IllegalArgumentException("Longitud inválida");
        }
    }
}