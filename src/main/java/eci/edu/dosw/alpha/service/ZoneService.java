package eci.edu.dosw.alpha.service;

import eci.edu.dosw.alpha.dto.ZoneRequest;
import eci.edu.dosw.alpha.dto.ZoneResponse;
import eci.edu.dosw.alpha.exception.InvalidZoneException;
import eci.edu.dosw.alpha.model.CampusZone;
import eci.edu.dosw.alpha.model.UserZonePreference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ZoneService {

    private final ConcurrentHashMap<String, UserZonePreference> preferences = new ConcurrentHashMap<>();

    /**
     * Flujo básico RF29: valida catálogo → guarda zona → retorna currentZone + nearbyParches
     */
    public ZoneResponse saveZone(String userId, ZoneRequest request) {
        CampusZone zone = parseZone(request.getCampusZone());

        UserZonePreference pref = new UserZonePreference();
        pref.setUserId(userId);
        pref.setCampusZone(zone);
        pref.setGeoLocationEnabled(request.isGeoLocationEnabled());
        pref.setUpdatedAt(System.currentTimeMillis());

        preferences.put(userId, pref);
        return buildResponse(pref);
    }

    /**
     * E2: si geoLocationEnabled=false no se usa zona → retorna nulls
     */
    public ZoneResponse getZone(String userId) {
        UserZonePreference pref = preferences.get(userId);
        if (pref == null) return null;
        return buildResponse(pref);
    }

    private CampusZone parseZone(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidZoneException("<vacío>");
        }
        try {
            return CampusZone.valueOf(raw.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidZoneException(raw);
        }
    }

    private ZoneResponse buildResponse(UserZonePreference pref) {
        // E2: ubicación desactivada
        if (!pref.isGeoLocationEnabled()) {
            return new ZoneResponse(null, List.of());
        }
        List<String> nearby = pref.getCampusZone().getNearby()
                .stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return new ZoneResponse(pref.getCampusZone().name(), nearby);
    }
}
