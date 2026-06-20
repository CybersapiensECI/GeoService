package eci.edu.dosw.alpha.controller;

import eci.edu.dosw.alpha.dto.ZoneRequest;
import eci.edu.dosw.alpha.dto.ZoneResponse;
import eci.edu.dosw.alpha.model.CampusZone;
import eci.edu.dosw.alpha.service.ZoneService;
import eci.edu.dosw.alpha.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/zone")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    /**
     * Flujo RF29: seleccionar zona + activar/desactivar geolocalización.
     * Input:  Authorization: Bearer <JWT>, body { campusZone, geoLocationEnabled }
     * Output: { currentZone, nearbyParches }
     * E1: zona inválida → 400 (manejado por GlobalExceptionHandler)
     */
    @PostMapping
    public ResponseEntity<ZoneResponse> saveZone(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ZoneRequest request) {

        String userId = JwtUtil.extractUserId(authHeader);
        ZoneResponse response = zoneService.saveZone(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene la zona actual del usuario autenticado.
     * E2: si geoLocationEnabled=false, currentZone=null y nearbyParches=[]
     */
    @GetMapping("/me")
    public ResponseEntity<ZoneResponse> getMyZone(
            @RequestHeader("Authorization") String authHeader) {

        String userId = JwtUtil.extractUserId(authHeader);
        ZoneResponse response = zoneService.getZone(userId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Catálogo de zonas disponibles (precondición RF29).
     * No requiere JWT — es información pública para el selector del frontend.
     */
    @GetMapping("/catalog")
    public ResponseEntity<List<String>> getCatalog() {
        List<String> zones = Arrays.stream(CampusZone.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(zones);
    }
}
