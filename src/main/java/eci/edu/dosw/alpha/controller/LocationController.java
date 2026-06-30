package eci.edu.dosw.alpha.controller;

import eci.edu.dosw.alpha.model.LocationMessage;
import eci.edu.dosw.alpha.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Ubicación", description = "Ubicación en tiempo real de usuarios vía WebSocket (/app/location → /topic/locations)")
@Controller
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @Operation(summary = "Recibir ubicación vía WebSocket",
               description = "Punto WebSocket: el cliente envía a /app/location y el servidor hace broadcast a /topic/locations. "
                           + "Valida userId, latitud ([-90,90]) y longitud ([-180,180]).")
    @ApiResponse(responseCode = "200", description = "Ubicación procesada y retransmitida")
    @MessageMapping("/location")
    @SendTo("/topic/locations")
    public LocationMessage receiveLocation(LocationMessage message) {
        return locationService.updateLocation(message);
    }

    @Operation(summary = "Obtener última ubicación de un usuario")
    @ApiResponse(responseCode = "200", description = "Última ubicación registrada (null si no hay datos)")
    @GetMapping("/{userId}")
    public LocationMessage getLocation(
            @Parameter(description = "ID del usuario") @PathVariable String userId) {
        return locationService.getLocation(userId);
    }
}
