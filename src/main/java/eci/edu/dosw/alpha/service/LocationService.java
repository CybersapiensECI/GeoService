package eci.edu.dosw.alpha.service;

import eci.edu.dosw.alpha.model.LocationMessage;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocationService {

    // Última ubicación por usuario
    private final ConcurrentHashMap<String, LocationMessage> locations = new ConcurrentHashMap<>();

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
        return message;
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