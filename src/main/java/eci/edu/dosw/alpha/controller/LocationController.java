package eci.edu.dosw.alpha.controller;


import eci.edu.dosw.alpha.model.LocationMessage;
import eci.edu.dosw.alpha.service.LocationService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @MessageMapping("/location") // cliente envía a /app/location
    @SendTo("/topic/locations") // broadcast a todos
    public LocationMessage receiveLocation(LocationMessage message) {

        // guardar y retornar
        return locationService.updateLocation(message);
    }
}