package eci.edu.dosw.alpha.service;

import eci.edu.dosw.alpha.model.LocationMessage;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocationService {

    // guardar última ubicación por usuario (en memoria)
    private final ConcurrentHashMap<String, LocationMessage> locations = new ConcurrentHashMap<>();

    public LocationMessage updateLocation(LocationMessage message) {
        locations.put(message.getUserId(), message);
        return message;
    }

    public LocationMessage getLocation(String userId) {
        return locations.get(userId);
    }
}
