package eci.edu.dosw.alpha.controller;

import eci.edu.dosw.alpha.dto.ZoneRequest;
import eci.edu.dosw.alpha.dto.ZoneResponse;
import eci.edu.dosw.alpha.model.CampusZone;
import eci.edu.dosw.alpha.service.ZoneService;
import eci.edu.dosw.alpha.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Zona", description = "Geolocalización simplificada en campus (RF29): selección de zona y preferencias de ubicación")
@RestController
@RequestMapping("/api/zone")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @Operation(summary = "Guardar zona del usuario",
               description = "Registra la zona del campus seleccionada por el usuario y su preferencia de geolocalización. "
                           + "E1: zona no válida → 400. E2: geoLocationEnabled=false → currentZone=null y nearbyParches=[].")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Zona guardada. Devuelve zona actual y parches cercanos"),
        @ApiResponse(responseCode = "400", description = "E1: zona inválida — consulte /catalog para zonas disponibles")
    })
    @PostMapping
    public ResponseEntity<ZoneResponse> saveZone(
            @Parameter(description = "Bearer JWT del usuario autenticado", required = true)
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ZoneRequest request) {

        String userId = JwtUtil.extractUserId(authHeader);
        ZoneResponse response = zoneService.saveZone(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener zona actual del usuario",
               description = "Devuelve la zona registrada del usuario autenticado. "
                           + "E2: si geolocalización desactivada → currentZone=null, nearbyParches=[].")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Zona del usuario"),
        @ApiResponse(responseCode = "404", description = "El usuario aún no ha registrado su zona")
    })
    @GetMapping("/me")
    public ResponseEntity<ZoneResponse> getMyZone(
            @Parameter(description = "Bearer JWT del usuario autenticado", required = true)
            @RequestHeader("Authorization") String authHeader) {

        String userId = JwtUtil.extractUserId(authHeader);
        ZoneResponse response = zoneService.getZone(userId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Catálogo de zonas disponibles",
               description = "Lista todas las zonas válidas del campus. No requiere autenticación. Úselo para poblar el selector del frontend.")
    @ApiResponse(responseCode = "200", description = "Lista de nombres de zonas del campus")
    @GetMapping("/catalog")
    public ResponseEntity<List<String>> getCatalog() {
        List<String> zones = Arrays.stream(CampusZone.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(zones);
    }
}
